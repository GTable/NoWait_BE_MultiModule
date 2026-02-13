package com.nowait.applicationuser.waiting.redis;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nowait.applicationuser.waiting.dto.IdempotencyResponse;
import com.nowait.applicationuser.waiting.dto.WaitingCancelIdempotencyValue;
import com.nowait.applicationuser.waiting.dto.WaitingIdempotencyValue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@RequiredArgsConstructor
@Slf4j
public class WaitingIdempotencyRepository {

	private final RedisTemplate<String, String> redisTemplate;
	private final ObjectMapper objectMapper;

	private static final Duration TTL = Duration.ofMinutes(10);

	// 멱등키 조회 메서드
	public Optional<WaitingIdempotencyValue> findByRegisterKey(String key) {
		String idempotencyValue = redisTemplate.opsForValue().get(key);

		if (idempotencyValue == null) {
			return Optional.empty();
		}

		try {
			log.info("Idempotency value found in Redis for key {}: {}", key, idempotencyValue);
			return Optional.of(objectMapper.readValue(idempotencyValue, WaitingIdempotencyValue.class));
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to deserialize value from Redis", e);
		}
	}

	// 멱등키 조회 메서드
	public WaitingCancelIdempotencyValue findByCancelKey(String key) {
		String idempotencyValue = redisTemplate.opsForValue().get(key);

		if (idempotencyValue == null) {
			return null;
		}

		try {
			log.info("Idempotency value found in Redis for key {}: {}", key, idempotencyValue);
			return objectMapper.readValue(idempotencyValue, WaitingCancelIdempotencyValue.class);
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to deserialize value from Redis", e);
		}
	}


	// 멱등키 저장 메서드
	public void saveIdempotencyResponse(String key, Object response) {
		IdempotencyResponse idempotencyResponse = new IdempotencyResponse(
			"COMPLETED",
			response
		);

		try {
			String jsonValue = objectMapper.writeValueAsString(idempotencyResponse);
			redisTemplate.opsForValue().set(key, jsonValue, TTL);
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to serialize value for Redis", e);
		}
	}

	public void saveIdempotencyInProgress(String key) {
		IdempotencyResponse idempotencyResponse = new IdempotencyResponse(
			"IN-PROGRESS",
			null
		);

		try {
			String jsonValue = objectMapper.writeValueAsString(idempotencyResponse);
			redisTemplate.opsForValue().set(key, jsonValue, TTL);
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to serialize value for Redis", e);
		}
	}
}
