package com.nowait.applicationuser.reservation.service;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.nowait.domaincoreredis.common.util.RedisKeyUtils;

@Testcontainers
public class WaitingPermitLuaRepositoryTest {

	@Container
	static GenericContainer<?> redis = new GenericContainer<>("redis:7.2-alpine").withExposedPorts(6379);

	static StringRedisTemplate template;
	static WaitingPermitLuaRepository permitRepo;

	final String userId = "U1";
	final Duration ttlTo3am = Duration.ofHours(6); // 테스트용
	final long leaseMs = 30_000;

	@BeforeAll
	static void setupAll() {
		var factory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
		factory.afterPropertiesSet();
		template = new StringRedisTemplate(factory);
		template.afterPropertiesSet();
		permitRepo = new WaitingPermitLuaRepository(template);
	}

	@BeforeEach
	void flush() {
		template.getConnectionFactory().getConnection().serverCommands().flushAll();
	}

	@Test
	void acquireLease_underLimit_succeedsUpTo3() {
		String hk = RedisKeyUtils.buildUserHoldingKey(userId);
		String ak = RedisKeyUtils.buildUserActiveKey(userId);
		long now = System.currentTimeMillis();

		// 1,2,3번째 임대 성공
		assertThat(permitRepo.acquireLease(userId, "t1", now, leaseMs, 3, ttlTo3am)).isTrue();
		assertThat(permitRepo.acquireLease(userId, "t2", now, leaseMs, 3, ttlTo3am)).isTrue();
		assertThat(permitRepo.acquireLease(userId, "t3", now, leaseMs, 3, ttlTo3am)).isTrue();

		// 4번째는 실패
		assertThat(permitRepo.acquireLease(userId, "t4", now, leaseMs, 3, ttlTo3am)).isFalse();

		// holding 3개 확인
		Long holding = template.opsForZSet().zCard(hk);
		Long active  = template.opsForSet().size(ak);
		assertThat(holding).isEqualTo(3L);
		assertThat(active).isEqualTo(0L);
	}

	@Test
	void finalize_movesFromHoldingToActive_totalCountMaintained() {
		long now = System.currentTimeMillis();
		// 두 개 임대 획득
		assertThat(permitRepo.acquireLease(userId, "t1", now, leaseMs, 3, ttlTo3am)).isTrue();
		assertThat(permitRepo.acquireLease(userId, "t2", now, leaseMs, 3, ttlTo3am)).isTrue();

		// t1 확정
		permitRepo.finalizeActive(userId, "t1", "10", "10-20250101-0001", ttlTo3am);

		String hk = RedisKeyUtils.buildUserHoldingKey(userId);
		String ak = RedisKeyUtils.buildUserActiveKey(userId);

		assertThat(template.opsForZSet().zCard(hk)).isEqualTo(1L); // t2만 holding
		assertThat(template.opsForSet().size(ak)).isEqualTo(1L);   // active 1
		assertThat(template.opsForSet().isMember(ak, "10:10-20250101-0001")).isTrue();
	}

	@Test
	void releaseLease_removesFromHolding() {
		long now = System.currentTimeMillis();
		assertThat(permitRepo.acquireLease(userId, "t1", now, leaseMs, 3, ttlTo3am)).isTrue();
		permitRepo.releaseLease(userId, "t1");

		String hk = RedisKeyUtils.buildUserHoldingKey(userId);
		assertThat(template.opsForZSet().zCard(hk)).isEqualTo(0L);
	}

	@Test
	void getActiveMembers_returnsSetMembers() {
		long now = System.currentTimeMillis();
		permitRepo.acquireLease(userId, "t1", now, leaseMs, 3, ttlTo3am);
		permitRepo.finalizeActive(userId, "t1", "77", "77-20250101-0001", ttlTo3am);
		Set<String> ms = permitRepo.getActiveMembers(userId);
		assertThat(ms).containsExactly("77:77-20250101-0001");
	}
}
