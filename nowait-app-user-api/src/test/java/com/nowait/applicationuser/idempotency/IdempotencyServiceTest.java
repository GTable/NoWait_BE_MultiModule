package com.nowait.applicationuser.idempotency;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.nowait.applicationuser.waiting.dto.CancelWaitingRequest;
import com.nowait.applicationuser.waiting.dto.CancelWaitingResponse;
import com.nowait.applicationuser.waiting.dto.RegisterWaitingRequest;
import com.nowait.applicationuser.waiting.dto.RegisterWaitingResponse;
import com.nowait.common.enums.ReservationStatus;

@Testcontainers
class IdempotencyServiceTest {

	@Container
	static GenericContainer<?> redis = new GenericContainer<>("redis:7.2-alpine").withExposedPorts(6379);

	static StringRedisTemplate template;
	static IdempotencyService idempotencyService;

	static final String SCOPE = "waiting:register:1:store";
	static final RegisterWaitingResponse REGISTERED =
		RegisterWaitingResponse.builder().waitingNumber("20261111-1-0001").partySize(2).build();

	@BeforeAll
	static void setUpAll() {
		var factory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
		factory.afterPropertiesSet();
		template = new StringRedisTemplate(factory);
		idempotencyService = new IdempotencyService(template, Jackson2ObjectMapperBuilder.json().build());
	}

	@BeforeEach
	void flush() {
		template.getConnectionFactory().getConnection().serverCommands().flushAll();
	}

	@Test
	@DisplayName("같은 키로 동시에 들어온 요청은 한 번만 실행되고, 이후 요청은 저장된 응답을 받는다")
	void concurrentSameKey_runsOnce() throws Exception {
		String key = UUID.randomUUID().toString();
		AtomicInteger runs = new AtomicInteger();
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(10);
		List<Future<RegisterWaitingResponse>> futures = new ArrayList<>();

		for (int i = 0; i < 10; i++) {
			futures.add(pool.submit(() -> {
				start.await();
				return idempotencyService.execute(SCOPE, key, new RegisterWaitingRequest(2),
					RegisterWaitingResponse.class, () -> {
						runs.incrementAndGet();
						sleep(300);
						return REGISTERED;
					});
			}));
		}
		start.countDown();

		int succeeded = 0;
		int rejected = 0;
		for (Future<RegisterWaitingResponse> f : futures) {
			try {
				f.get();
				succeeded++;
			} catch (Exception e) {
				assertThat(e.getCause()).isInstanceOf(WorkInProgressException.class);
				rejected++;
			}
		}
		pool.shutdown();

		assertThat(runs).hasValue(1);
		assertThat(succeeded).isEqualTo(1);
		assertThat(rejected).isEqualTo(9);

		RegisterWaitingResponse replayed = idempotencyService.execute(SCOPE, key, new RegisterWaitingRequest(2),
			RegisterWaitingResponse.class, IdempotencyServiceTest::mustNotRun);
		assertThat(replayed).usingRecursiveComparison().isEqualTo(REGISTERED);
	}

	@Test
	@DisplayName("실패한 요청은 키를 풀어줘서 같은 키로 다시 시도할 수 있다")
	void failure_releasesKey() {
		String key = UUID.randomUUID().toString();
		CancelWaitingRequest request = new CancelWaitingRequest("20261111-1-0001");

		assertThatThrownBy(() -> idempotencyService.execute("waiting:cancel:1:store", key, request,
			CancelWaitingResponse.class, () -> {
				throw new IllegalStateException("DB 커밋 실패");
			})).isInstanceOf(IllegalStateException.class);

		CancelWaitingResponse ok = CancelWaitingResponse.builder()
			.waitingNumber("20261111-1-0001")
			.storeId(1L)
			.reservationStatus(ReservationStatus.CANCELLED)
			.canceledAt(LocalDateTime.of(2026, 11, 11, 12, 0))
			.message("대기 취소가 완료되었습니다.")
			.build();
		idempotencyService.execute("waiting:cancel:1:store", key, request, CancelWaitingResponse.class, () -> ok);

		CancelWaitingResponse replayed = idempotencyService.execute("waiting:cancel:1:store", key, request,
			CancelWaitingResponse.class, IdempotencyServiceTest::mustNotRun);
		assertThat(replayed).usingRecursiveComparison().isEqualTo(ok);
	}

	@Test
	@DisplayName("같은 키로 본문이 다른 요청이 오면 실행하지 않고 거절한다")
	void sameKeyDifferentBody_rejected() {
		String key = UUID.randomUUID().toString();
		idempotencyService.execute(SCOPE, key, new RegisterWaitingRequest(2), RegisterWaitingResponse.class,
			() -> REGISTERED);

		assertThatThrownBy(() -> idempotencyService.execute(SCOPE, key, new RegisterWaitingRequest(4),
			RegisterWaitingResponse.class, IdempotencyServiceTest::mustNotRun))
			.isInstanceOf(IdempotencyKeyMismatchException.class);
	}

	@Test
	@DisplayName("처리 중 키는 1분, 완료된 키는 3분 동안 유지된다")
	void ttl_differsByState() {
		String key = UUID.randomUUID().toString();
		String redisKey = "idempotency:" + SCOPE + ":" + key;

		idempotencyService.execute(SCOPE, key, new RegisterWaitingRequest(2), RegisterWaitingResponse.class, () -> {
			assertThat(template.getExpire(redisKey, TimeUnit.SECONDS)).isBetween(55L, 60L);
			return REGISTERED;
		});

		assertThat(template.getExpire(redisKey, TimeUnit.SECONDS)).isBetween(175L, 180L);
	}

	@Test
	@DisplayName("UUID 형식이 아닌 키는 Redis 에 쓰지 않고 거절한다")
	void invalidKeyFormat_rejected() {
		for (String key : List.of("abc", "waiting:sequence:1:20261111", "550e8400e29b41d4a716446655440000")) {
			assertThatThrownBy(() -> idempotencyService.execute(SCOPE, key, new RegisterWaitingRequest(2),
				RegisterWaitingResponse.class, IdempotencyServiceTest::mustNotRun))
				.isInstanceOf(InvalidIdempotencyKeyException.class);
		}
		assertThat(template.keys("*")).isEmpty();
	}

	private static <T> T mustNotRun() {
		throw new AssertionError("실행되면 안 된다");
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
