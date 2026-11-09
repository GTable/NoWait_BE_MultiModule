package com.nowait.applicationadmin.reservation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.nowait.applicationadmin.reservation.scheduler.WaitingRedisRecoveryScheduler;
import com.nowait.common.enums.ReservationStatus;
import com.nowait.domaincorerdb.reservation.entity.Reservation;
import com.nowait.domaincorerdb.reservation.repository.ReservationRepository;
import com.nowait.domaincorerdb.store.entity.Store;
import com.nowait.common.enums.Role;
import com.nowait.common.enums.SocialType;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domaincoreredis.reservation.repository.WaitingPermitLuaRepository;
import com.nowait.domaincoreredis.reservation.repository.WaitingRedisRepository;

@Testcontainers
class WaitingRedisRecoveryIT {

	@Container
	static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

	private static final Long STORE_ID = 23L;
	private static final LocalDateTime NOW = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

	private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
	private StringRedisTemplate template;
	private WaitingPermitLuaRepository waitingPermitLuaRepository;
	private WaitingRedisRecoveryScheduler scheduler;

	@BeforeEach
	void setUp() {
		LettuceConnectionFactory factory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
		factory.afterPropertiesSet();
		template = new StringRedisTemplate(factory);
		factory.getConnection().serverCommands().flushAll();
		waitingPermitLuaRepository = new WaitingPermitLuaRepository(template);
		scheduler = new WaitingRedisRecoveryScheduler(reservationRepository, new WaitingRedisRepository(template),
			waitingPermitLuaRepository);
	}

	private Reservation row(Long id, Long userId, String number, ReservationStatus status) {
		Store store = Store.builder().storeId(STORE_ID).departmentId(1L).name("S").location("L").description("D")
			.noticeTitle("T").noticeContent("C").openTime("10001100").isActive(true).deleted(false).build();
		User user = User.createUserWithId(userId, "u@a", "n", "i", SocialType.KAKAO, Role.USER, STORE_ID);
		return Reservation.builder().id(id).reservationNumber(number).store(store).user(user).partySize(4)
			.requestedAt(NOW).updatedAt(NOW).status(status).build();
	}

	@Test
	@DisplayName("Redis 를 잃으면 DB 의 진행 중 예약으로 대기열 전체를 복구한다")
	void restores_all_keys() {
		given(reservationRepository.findActiveSince(any(), any())).willReturn(List.of(
			row(10L, 10L, "23-20261109-0007", ReservationStatus.CALLING),
			row(11L, 11L, "20261109-23-0001", ReservationStatus.WAITING))); // v2 번호는 대상 아님
		given(reservationRepository.findById(10L)).willReturn(Optional.of(
			row(10L, 10L, "23-20261109-0007", ReservationStatus.CALLING)));

		scheduler.restoreMissingWaitings();

		assertThat(template.opsForZSet().zCard("waiting:23")).isEqualTo(1L);
		assertThat(template.opsForHash().get("waiting:status:23", "10")).isEqualTo("CALLING");
		assertThat(template.opsForHash().get("waiting:party:23", "10")).isEqualTo("4");
		assertThat(template.opsForHash().get("reservation:number:23", "10")).isEqualTo("23-20261109-0007");
		assertThat(template.opsForHash().get("reservation:user:23", "23-20261109-0007")).isEqualTo("10");
		assertThat(template.opsForHash().get("waiting:calledAt:23", "10")).isNotNull();
		assertThat(template.opsForSet().isMember("waiting:user:{10}:active", "23:23-20261109-0007")).isTrue();
		assertThat(template.opsForValue().get("reservation:seq:23:20261109")).isEqualTo("7");
		assertThat(template.getExpire("waiting:23")).isPositive();
	}

	@Test
	@DisplayName("이미 대기열에 있으면 바꾸지 않고, 일일 시퀀스를 낮추지 않는다")
	void keeps_existing_member_and_higher_sequence() {
		template.opsForZSet().add("waiting:23", "10", 1);
		template.opsForHash().put("waiting:status:23", "10", "CALLING");
		template.opsForValue().set("reservation:seq:23:20261109", "20");
		given(reservationRepository.findActiveSince(any(), any())).willReturn(List.of(
			row(10L, 10L, "23-20261109-0007", ReservationStatus.WAITING),
			row(12L, 12L, "23-20261109-0008", ReservationStatus.WAITING)));
		given(reservationRepository.findById(12L)).willReturn(Optional.of(
			row(12L, 12L, "23-20261109-0008", ReservationStatus.WAITING)));

		scheduler.restoreMissingWaitings();

		assertThat(template.opsForZSet().score("waiting:23", "10")).isEqualTo(1.0);
		assertThat(template.opsForHash().get("waiting:status:23", "10")).isEqualTo("CALLING");
		assertThat(template.opsForHash().get("waiting:status:23", "12")).isEqualTo("WAITING");
		assertThat(template.opsForValue().get("reservation:seq:23:20261109")).isEqualTo("20");
	}

	@Test
	@DisplayName("복구 직후 재확인에서 취소된 예약이면 대기열과 active 멤버를 지운다")
	void removes_waiting_when_cancelled_after_restore() {
		given(reservationRepository.findActiveSince(any(), any())).willReturn(List.of(
			row(13L, 13L, "23-20261109-0009", ReservationStatus.WAITING)));
		given(reservationRepository.findById(13L)).willReturn(Optional.of(
			row(13L, 13L, "23-20261109-0009", ReservationStatus.CANCELLED)));

		scheduler.restoreMissingWaitings();

		assertThat(template.opsForZSet().score("waiting:23", "13")).isNull();
		assertThat(waitingPermitLuaRepository.getActiveMembers("13"))
			.doesNotContain("23:23-20261109-0009");
	}
}
