package com.nowait.applicationuser.idempotency;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

	private static final String IN_PROGRESS = "IN-PROGRESS";
	private static final Duration TTL = Duration.ofMinutes(10);
	private static final Pattern UUID_FORMAT =
		Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	/**
	 * 같은 Idempotency-Key 요청은 action 을 한 번만 실행하고, 이후엔 저장된 응답을 돌려준다.
	 * 키가 비어 있으면 멱등 처리 없이 action 만 실행한다.
	 *
	 * @param scope   기능·사용자·경로 값을 구분하는 접두사 (예: "waiting:register:{userId}:{publicCode}").
	 *                클라이언트 키가 다른 Redis 키와 겹치지 않게 하고, 경로가 다르면 새 요청으로 본다
	 * @param request 같은 키로 다른 내용이 오면 거절하기 위해 지문을 뜨는 대상 (보통 요청 본문)
	 */
	public <T> T execute(String scope, String idempotencyKey, Object request, Class<T> responseType,
		Supplier<T> action) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			return action.get();
		}
		if (!UUID_FORMAT.matcher(idempotencyKey).matches()) {
			throw new InvalidIdempotencyKeyException();
		}
		// 트랜잭션 안에서 부르면 action 이 끝나도 아직 커밋 전이라, 커밋이 실패해도 "완료"가 남는다
		Assert.state(!TransactionSynchronizationManager.isActualTransactionActive(),
			"멱등 처리는 트랜잭션 바깥(컨트롤러 등)에서 호출해야 합니다");

		String key = "idempotency:" + scope + ":" + idempotencyKey;
		String fingerprint = fingerprint(request);

		// 값 형식: "{요청 지문}:{IN-PROGRESS | 응답 JSON}"
		// GET 후 SET 이면 그 사이에 같은 키 요청이 끼어든다. SET NX 한 번으로 선점한다
		if (!Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, fingerprint + ":" + IN_PROGRESS, TTL))) {
			return replay(key, fingerprint, responseType);
		}

		T response;
		try {
			response = action.get();
		} catch (RuntimeException e) {
			redisTemplate.delete(key); // 실패한 요청은 같은 키로 다시 시도할 수 있어야 한다
			throw e;
		}

		try {
			redisTemplate.opsForValue().set(key, fingerprint + ":" + objectMapper.writeValueAsString(response), TTL);
		} catch (Exception e) {
			// ponytail: 작업은 이미 커밋됐으니 응답은 그대로 준다. 키는 IN-PROGRESS 로 남아 TTL 동안 재시도를 막고,
			//  그 뒤 같은 키 재시도는 다시 실행된다. 이것까지 막으려면 DB 에 멱등키 UNIQUE 제약 추가
			log.error("멱등 응답 저장 실패 - key: {}", key, e);
		}
		return response;
	}

	private <T> T replay(String key, String fingerprint, Class<T> responseType) {
		String saved = redisTemplate.opsForValue().get(key);
		// null 이면 앞선 요청이 실패해 키를 막 풀어준 경우라 다시 시도하면 된다
		if (saved == null) {
			throw new WorkInProgressException();
		}
		String[] parts = saved.split(":", 2);
		if (!parts[0].equals(fingerprint)) {
			throw new IdempotencyKeyMismatchException();
		}
		if (IN_PROGRESS.equals(parts[1])) {
			throw new WorkInProgressException();
		}
		try {
			return objectMapper.readValue(parts[1], responseType);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("멱등 응답 역직렬화 실패 - key: " + key, e);
		}
	}

	private String fingerprint(Object request) {
		try {
			byte[] body = objectMapper.writeValueAsBytes(request);
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
		} catch (JsonProcessingException | NoSuchAlgorithmException e) {
			throw new IllegalStateException("멱등 요청 지문 생성 실패", e);
		}
	}
}
