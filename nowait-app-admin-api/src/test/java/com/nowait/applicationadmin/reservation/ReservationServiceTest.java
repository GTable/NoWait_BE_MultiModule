package com.nowait.applicationadmin.reservation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.nowait.applicationadmin.reservation.dto.EntryStatusResponseDto;
import com.nowait.applicationadmin.reservation.dto.WaitingUserResponse;
import com.nowait.applicationadmin.reservation.service.ReservationService;
import com.nowait.common.enums.ReservationStatus;
import com.nowait.common.enums.Role;
import com.nowait.common.enums.SocialType;
import com.nowait.domaincorerdb.reservation.entity.Reservation;
import com.nowait.domaincorerdb.reservation.repository.ReservationRepository;
import com.nowait.domaincorerdb.store.entity.Store;
import com.nowait.domaincorerdb.store.repository.StoreRepository;
import com.nowait.domaincorerdb.user.entity.MemberDetails;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domaincorerdb.user.repository.UserRepository;

public class ReservationServiceTest {

	@Mock
	private ReservationRepository reservationRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private WaitingRedisRepository waitingRedisRepository;
	@Mock
	private StoreRepository storeRepository;
	@Mock
	private RedisTemplate<String, Object> redisTemplate;

	@Mock
	private ZSetOperations zSetOps;


	@InjectMocks
	private ReservationService reservationService;

	private final Long storeId = 23L;
	private final String userId = "10";
	private final AtomicLong seq = new AtomicLong(1000L);
	private MemberDetails adminPrincipal;
	private Store storeRef;

	private Answer<Reservation> stubSaveAssigningId() {
		return inv -> {
			Reservation r = inv.getArgument(0);
			// 새 객체 만들어 ID 포함해서 반환
			return Reservation.builder()
				.id(seq.incrementAndGet())
				.reservationNumber(r.getReservationNumber())
				.store(r.getStore())
				.user(r.getUser())
				.requestedAt(r.getRequestedAt())
				.updatedAt(r.getUpdatedAt())
				.status(r.getStatus())
				.partySize(r.getPartySize())
				.build();
		};
	}

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		adminPrincipal = MemberDetails.builder()
			.id(1L).email("admin@test.com").password("pw")
			.authorities(List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
			.build();

		User adminUser = User.createUserWithId(
			1L, "admin@test.com", "admin", "img",
			SocialType.LOCAL, Role.SUPER_ADMIN, storeId
		);

		given(userRepository.findById(1L)).willReturn(Optional.of(adminUser));

		storeRef = Store.builder()
			.storeId(storeId)
			.departmentId(10L)
			.name("S")
			.location("L")
			.description("D")
			.noticeTitle("NT")
			.noticeContent("NC")
			.openTime("10001100")
			.isActive(true)
			.deleted(false)
			.build();

		given(storeRepository.getReferenceById(storeId)).willReturn(storeRef);

		given(redisTemplate.opsForZSet()).willReturn(zSetOps);
	}

	@Test
	@DisplayName("WAITING → CALLING")
	void process_waiting_to_calling() {
		// given
		given(waitingRedisRepository.getReservationId(storeId, userId)).willReturn("R-001");
		given(waitingRedisRepository.getWaitingStatus(storeId, userId)).willReturn("WAITING");
		given(waitingRedisRepository.getWaitingPartySize(storeId, userId)).willReturn(3);
		given(zSetOps.score("waiting:" + storeId, userId)).willReturn((double) System.currentTimeMillis());
		User userRef = User.createUserWithId(200L, "u@a", "neo", "img", SocialType.KAKAO, Role.USER, storeId);
		given(userRepository.getReferenceById(anyLong())).willReturn(userRef);


		// when
		EntryStatusResponseDto dto = reservationService.processEntryStatus(
			storeId, userId, adminPrincipal, ReservationStatus.CALLING);

		// then
		assertThat(dto.getStatus()).isEqualTo("CALLING");
		assertThat(dto.getReservationNumber()).isEqualTo("R-001");
		then(waitingRedisRepository).should().setWaitingStatus(storeId, userId, "CALLING");
		then(waitingRedisRepository).should().setWaitingCalledAt(eq(storeId), eq(userId), anyLong());
		then(reservationRepository).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("WAITING → CONFIRMED : DB 저장 + Redis 삭제")
	void process_waiting_to_confirmed() {
		// given
		given(waitingRedisRepository.getReservationId(storeId, userId)).willReturn("R-002");
		given(waitingRedisRepository.getWaitingStatus(storeId, userId)).willReturn("WAITING");
		given(waitingRedisRepository.getWaitingPartySize(storeId, userId)).willReturn(2);
		given(zSetOps.score("waiting:" + storeId, userId)).willReturn((double) System.currentTimeMillis());
		given(userRepository.getReferenceById(anyLong()))
			.willReturn(User.createUserWithId(200L, "u@a", "neo", "img", SocialType.KAKAO, Role.USER, storeId));

		AtomicReference<Reservation> savedRef = new AtomicReference<>();
		given(reservationRepository.save(any(Reservation.class))).willAnswer(inv -> {
			Reservation saved = stubSaveAssigningId().answer(inv); // ★ ID 있는 객체
			savedRef.set(saved);
			return saved;
		});

		// when
		EntryStatusResponseDto dto = reservationService.processEntryStatus(
			storeId, userId, adminPrincipal, ReservationStatus.CONFIRMED);

		// then
		then(waitingRedisRepository).should().deleteWaiting(storeId, userId);
		assertThat(savedRef.get().getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
		assertThat(dto.getStatus()).isEqualTo("CONFIRMED");
		assertThat(dto.getReservationNumber()).isEqualTo("R-002");
	}


	@Test
	@DisplayName("CALLING → CONFIRMED : DB 저장 + Redis 삭제")
	void process_calling_to_confirmed() {
		// given
		given(waitingRedisRepository.getReservationId(storeId, userId)).willReturn("R-003");
		given(waitingRedisRepository.getWaitingStatus(storeId, userId)).willReturn("CALLING");
		given(waitingRedisRepository.getWaitingPartySize(storeId, userId)).willReturn(4);
		given(zSetOps.score("waiting:" + storeId, userId)).willReturn((double) System.currentTimeMillis());
		given(userRepository.getReferenceById(anyLong()))
			.willReturn(User.createUserWithId(200L, "u@a", "neo", "img", SocialType.KAKAO, Role.USER, storeId));
		given(reservationRepository.save(any(Reservation.class))).willAnswer(stubSaveAssigningId()); // ★

		// when
		EntryStatusResponseDto dto = reservationService.processEntryStatus(
			storeId, userId, adminPrincipal, ReservationStatus.CONFIRMED);

		// then
		then(waitingRedisRepository).should().deleteWaiting(storeId, userId);
		assertThat(dto.getStatus()).isEqualTo("CONFIRMED");
	}


	@Test
	@DisplayName("CANCELLED 상태에서 CALLING 시도 → 예외")
	void process_cancelled_to_calling_illegal() {
		// given
		given(waitingRedisRepository.getWaitingStatus(storeId, userId)).willReturn("CANCELLED");

		// when / then
		assertThatThrownBy(() ->
			reservationService.processEntryStatus(storeId, userId, adminPrincipal, ReservationStatus.CALLING)
		).isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("WAITING → CANCELLED : DB 저장 + Redis 삭제")
	void process_waiting_to_cancelled() {
		// given
		given(waitingRedisRepository.getReservationId(storeId, userId)).willReturn("R-004");
		given(waitingRedisRepository.getWaitingStatus(storeId, userId)).willReturn("WAITING");
		given(waitingRedisRepository.getWaitingPartySize(storeId, userId)).willReturn(2);
		given(zSetOps.score("waiting:" + storeId, userId)).willReturn((double) System.currentTimeMillis());
		given(userRepository.getReferenceById(anyLong()))
			.willReturn(User.createUserWithId(200L, "u@a", "neo", "img", SocialType.KAKAO, Role.USER, storeId));
		given(reservationRepository.save(any(Reservation.class))).willAnswer(stubSaveAssigningId()); // ★

		// when
		EntryStatusResponseDto dto = reservationService.processEntryStatus(
			storeId, userId, adminPrincipal, ReservationStatus.CANCELLED);

		// then
		then(waitingRedisRepository).should().deleteWaiting(storeId, userId);
		assertThat(dto.getStatus()).isEqualTo("CANCELLED");
	}


	@Test
	@DisplayName("이미 CANCELLED 기록 존재 시 → CONFIRMED로 전환 (DB 조회 분기)")
	void process_from_cancelled_record_to_confirmed() {
		// given
		given(waitingRedisRepository.getWaitingStatus(storeId, userId)).willReturn("CANCELLED");
		Long uid = 200L;

		User userRef = User.createUserWithId(uid, "u@a", "neo", "img", SocialType.KAKAO, Role.USER, storeId);
		Reservation cancelled = Reservation.builder()
			.id(777L)
			.reservationNumber("R-005")
			.store(storeRef)
			.user(userRef)
			.requestedAt(LocalDateTime.now().minusMinutes(30))
			.updatedAt(LocalDateTime.now().minusMinutes(20))
			.status(ReservationStatus.CANCELLED)
			.partySize(3)
			.build();

		given(reservationRepository.findFirstByStore_StoreIdAndUserIdAndStatusInAndRequestedAtBetweenOrderByRequestedAtDesc(
			eq(storeId), eq(uid), eq(List.of(ReservationStatus.CANCELLED)), any(), any()
		)).willReturn(Optional.of(cancelled));
		given(userRepository.getReferenceById(anyLong())).willReturn(userRef);
		given(reservationRepository.save(any(Reservation.class))).willAnswer(stubSaveAssigningId()); // ★

		// when
		EntryStatusResponseDto dto = reservationService.processEntryStatus(
			storeId, String.valueOf(uid), adminPrincipal, ReservationStatus.CONFIRMED);

		// then
		then(reservationRepository).should().save(any(Reservation.class));
		assertThat(dto.getStatus()).isEqualTo("CONFIRMED");
		assertThat(dto.getReservationNumber()).isEqualTo("R-005");
	}


	@Test
	@DisplayName("getAllWaitingUserDetails : 파이프라인 매핑")
	void get_all_waiting_pipeline_mapping() {
		// given
		long t1 = System.currentTimeMillis() - 1000;
		long t2 = System.currentTimeMillis();
		var a = new DefaultTypedTuple<>("200", (double) t1);
		var b = new DefaultTypedTuple<>("201", (double) t2);
		given(waitingRedisRepository.getAllWaitingWithScore(storeId)).willReturn(List.of(a, b));

		given(userRepository.findAllById(Arrays.asList(200L, 201L)))
			.willReturn(List.of(
				User.createUserWithId(200L, "u1@a", "neo", "img", SocialType.KAKAO, Role.USER, storeId),
				User.createUserWithId(201L, "u2@a", "trinity", "img", SocialType.KAKAO, Role.USER, storeId)
			));

		List<Object> pipeline = new ArrayList<>();
		pipeline.add("3"); pipeline.add("WAITING");  pipeline.add("R-100"); pipeline.add(String.valueOf(t1)); pipeline.add((double) t1);
		pipeline.add("2"); pipeline.add("CALLING");  pipeline.add("R-101"); pipeline.add(String.valueOf(t2)); pipeline.add((double) t2);
		given(redisTemplate.executePipelined(any(RedisCallback.class))).willReturn(pipeline);

		// when
		List<WaitingUserResponse> list = reservationService.getAllWaitingUserDetails(storeId);

		// then
		assertThat(list).hasSize(2);
		assertThat(list.get(0).getUserId()).isEqualTo("200");
		assertThat(list.get(0).getUserName()).isEqualTo("neo");
		assertThat(list.get(0).getReservationNumber()).isEqualTo("R-100");
		assertThat(list.get(0).getStatus()).isEqualTo("WAITING");
		assertThat(list.get(1).getStatus()).isEqualTo("CALLING");
		assertThat(list.get(0).getCreatedAt()).isBefore(list.get(1).getCreatedAt());
	}
}
