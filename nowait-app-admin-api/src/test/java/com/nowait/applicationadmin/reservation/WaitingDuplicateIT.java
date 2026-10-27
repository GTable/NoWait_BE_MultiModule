package com.nowait.applicationadmin.reservation;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.nowait.domaincoreredis.common.util.RedisKeyUtils;

@Testcontainers
public class WaitingDuplicateIT {
	@Container
	static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

	StringRedisTemplate tpl;

	Long storeId = 100L;
	String zKey, stKey, partyKey, numKey, calledKey;

	@BeforeEach
	void init() {
		var cfg = new RedisStandaloneConfiguration("localhost", redis.getMappedPort(6379));
		var lf = new LettuceConnectionFactory(cfg);
		lf.afterPropertiesSet();
		tpl = new StringRedisTemplate(lf);

		zKey = RedisKeyUtils.buildWaitingKeyPrefix() + storeId;            // waiting:{storeId}
		stKey = RedisKeyUtils.buildWaitingStatusKeyPrefix() + storeId;     // waiting:status:{storeId}
		partyKey = RedisKeyUtils.buildWaitingPartySizeKeyPrefix() + storeId; // waiting:party:{storeId}
		numKey = RedisKeyUtils.buildReservationNumberKey(storeId);         // reservation:number:{storeId}
		calledKey = RedisKeyUtils.buildWaitingCalledAtKeyPrefix() + storeId; // waiting:calledAt:{storeId}

		// clean
		tpl.delete(List.of(zKey, stKey, partyKey, numKey, calledKey));
	}

	// ===== Helper: 현재 목록 덤프 =====
	private List<String> dump() {
		Set<String> ids = tpl.opsForZSet().range(zKey, 0, -1);
		if (ids == null) return List.of();
		List<String> out = new ArrayList<>();
		for (String uid : ids) {
			String status = (String) tpl.opsForHash().get(stKey, uid);
			String party = (String) tpl.opsForHash().get(partyKey, uid);
			String num = (String) tpl.opsForHash().get(numKey, uid);
			out.add(uid + "|" + status + "|" + party + "|" + num);
		}
		return out;
	}

	// ====== 시나리오 A: ID 정규화 실패(공백 차이) ======
	@Test
	@DisplayName("A) userId 공백/포맷 불일치 → 사실상 같은 팀이 두 줄로 보임")
	void duplicate_by_userid_format() {
		// given
		String uid1 = "200";
		String uid2 = " 200"; // 앞 공백 (의도적인 비정규화)
		long now = System.currentTimeMillis();

		// when
		tpl.opsForZSet().add(zKey, uid1, now);
		tpl.opsForHash().put(stKey, uid1, "WAITING");
		tpl.opsForHash().put(partyKey, uid1, "3");
		tpl.opsForHash().put(numKey, uid1, "R-100");

		tpl.opsForZSet().add(zKey, uid2, now + 1); // 공백 다른 멤버 → ZSET에선 “다른 사용자”
		tpl.opsForHash().put(stKey, uid2, "WAITING");
		tpl.opsForHash().put(partyKey, uid2, "3");
		tpl.opsForHash().put(numKey, uid2, "R-100"); // 같은 예약번호까지

		// then
		List<String> view = dump();
		assertThat(view).hasSize(2); // UI 관점엔 중복으로 보임
		// 방어 로직 없다면 동일 사용자로 인식될 수 있음
	}

	// ====== 시나리오 B: 예약번호 충돌(비원자 생성/할당 가정) ======
	@Test
	@DisplayName("B) 예약번호 충돌 → 서로 다른 userId가 같은 reservationNumber로 노출")
	void duplicate_by_reservation_number_collision() throws Exception {
		// given
		String u1 = "201";
		String u2 = "202";
		long t = System.currentTimeMillis();

		// when (의도적으로 같은 번호 세팅)
		tpl.opsForZSet().add(zKey, u1, t);
		tpl.opsForHash().put(stKey, u1, "WAITING");
		tpl.opsForHash().put(partyKey, u1, "2");
		tpl.opsForHash().put(numKey, u1, "R-200");

		tpl.opsForZSet().add(zKey, u2, t + 1);
		tpl.opsForHash().put(stKey, u2, "WAITING");
		tpl.opsForHash().put(partyKey, u2, "2");
		tpl.opsForHash().put(numKey, u2, "R-200"); // 충돌

		// then
		List<String> view = dump();
		assertThat(view).filteredOn(s -> s.endsWith("|R-200")).hasSize(2); // 같은 번호 2개
		// DB unique는 막지만 Redis 뷰에선 중복 그대로 노출됨
	}

	// ====== 시나리오 C: 부분 삭제 레이스(유령 조각) ======
	@Test
	@DisplayName("C) 부분 삭제 실패 → 찌꺼기 남아 중복/유령 항목 노출")
	void ghost_after_partial_delete() {
		// given
		String uid = "203";
		long t = System.currentTimeMillis();
		tpl.opsForZSet().add(zKey, uid, t);
		tpl.opsForHash().put(stKey, uid, "WAITING");
		tpl.opsForHash().put(partyKey, uid, "4");
		tpl.opsForHash().put(numKey, uid, "R-300");

		// when (의도적으로 해시만 지우고 ZSET은 남김 = 네 현재 deleteWaiting 순차 호출이 실패한 상황을 흉내)
		tpl.opsForHash().delete(stKey, uid);
		tpl.opsForHash().delete(partyKey, uid);
		tpl.opsForHash().delete(numKey, uid);
		// tpl.opsForZSet().remove(zKey, uid); // 실패했다고 가정

		// then
		var ids = tpl.opsForZSet().range(zKey, 0, -1);
		assertThat(ids).contains(uid); // 큐엔 남아있음
		List<String> view = dump();
		// status/party/number가 null → 조회/매핑에서 NPE or 비정상 라인 가능
		assertThat(view.get(0)).contains("null");
	}

	// ====== 시나리오 D(선택): 동시 등록 레이스 증폭 ======
	@Test
	@DisplayName("D) 동시 등록 100개(같은 사용자/다른 포맷) → 중복 노출 가능성 확인")
	void concurrent_register_format_mixture() throws Exception {
		// given
		ExecutorService es = Executors.newFixedThreadPool(16);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(100);

		// when
		for (int i = 0; i < 100; i++) {
			final int k = i;
			es.submit(() -> {
				try {
					start.await();
					String uid = (k % 2 == 0) ? "200" : " 200"; // 포맷 섞기
					long score = System.currentTimeMillis() + k;
					tpl.opsForZSet().add(zKey, uid, score);
					tpl.opsForHash().put(stKey, uid, "WAITING");
					tpl.opsForHash().put(partyKey, uid, "3");
					tpl.opsForHash().put(numKey, uid, "R-BURST");
				} catch (Exception ignore) {
				} finally {
					done.countDown();
				}
			});
		}
		start.countDown();
		done.await(10, TimeUnit.SECONDS);
		es.shutdown();

		// then
		var view = dump();
		// 공백/포맷이 다른 멤버가 둘 다 남아있어 2줄 이상일 가능성
		assertThat(view.stream().filter(s -> s.endsWith("|R-BURST")).count()).isGreaterThanOrEqualTo(2);
	}
}
