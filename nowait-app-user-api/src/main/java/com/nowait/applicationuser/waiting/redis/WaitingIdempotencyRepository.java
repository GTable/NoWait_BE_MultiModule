package com.nowait.applicationuser.waiting.redis;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nowait.applicationuser.waiting.dto.CancelWaitingResponse;
import com.nowait.applicationuser.waiting.dto.RegisterWaitingResponse;
import com.nowait.applicationuser.waiting.dto.WaitingCancelIdempotencyValue;
import com.nowait.applicationuser.waiting.dto.WaitingIdempotencyValue;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class WaitingIdempotencyRepository {

	private final RedisTemplate<String, String> redisTemplate;
	private final ObjectMapper objectMapper;

	private static final Duration TTL = Duration.ofMinutes(10);

	// 멱등키 조회 메서드
	public Optional<WaitingIdempotencyValue> findByKey(String key) {
		String idempotencyValue = redisTemplate.opsForValue().get(key);

		if (idempotencyValue == null) {
			return Optional.empty();
		}

		try {
			return Optional.of(
				objectMapper.readValue(idempotencyValue, WaitingIdempotencyValue.class)
			);
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to deserialize value from Redis", e);
		}
	}

	// 멱등키 조회 메서드
	public Optional<WaitingCancelIdempotencyValue> findByCancelKey(String key) {
		String idempotencyValue = redisTemplate.opsForValue().get(key);

		if (idempotencyValue == null) {
			return Optional.empty();
		}

		try {
			return Optional.of(
				objectMapper.readValue(idempotencyValue, WaitingCancelIdempotencyValue.class)
			);
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to deserialize value from Redis", e);
		}
	}


	// 멱등키 저장 메서드 - 대기 등록
	public void saveIdempotencyValue(String key, RegisterWaitingResponse response) {
		WaitingIdempotencyValue waitingIdempotencyValue = new WaitingIdempotencyValue(
			"COMPLETED",
			response
		);

		try {
			String jsonValue = objectMapper.writeValueAsString(waitingIdempotencyValue);
			redisTemplate.opsForValue().set(key, jsonValue, TTL);
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to serialize value for Redis", e);
		}
	}

	// 멱등키 저장 메서드 - 대기 취소
	public void saveCancelIdempotencyValue(String key, CancelWaitingResponse response) {
		WaitingCancelIdempotencyValue waitingIdempotencyValue = new WaitingCancelIdempotencyValue(
			"COMPLETED",
			response
		);

		try {
			String jsonValue = objectMapper.writeValueAsString(waitingIdempotencyValue);
			redisTemplate.opsForValue().set(key, jsonValue, TTL);
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to serialize value for Redis", e);
		}
	}
}
