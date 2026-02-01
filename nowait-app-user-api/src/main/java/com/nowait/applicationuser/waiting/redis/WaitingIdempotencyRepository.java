package com.nowait.applicationuser.waiting.redis;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nowait.applicationuser.waiting.dto.RegisterWaitingResponse;
import com.nowait.applicationuser.waiting.dto.WaitingIdempotencyValue;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class WaitingIdempotencyRepository {

	private final RedisTemplate<String, String> redisTemplate;
	private final ObjectMapper objectMapper;

	private static final Duration TTL = Duration.ofMinutes(10);

	public Optional<WaitingIdempotencyValue> findByKey(String key) {
		String value = redisTemplate.opsForValue().get(key);

		if (value == null) {
			return Optional.empty();
		}

		try {
			return Optional.of(
				objectMapper.readValue(value, WaitingIdempotencyValue.class)
			);
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to deserialize value from Redis", e);
		}
	}

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
}
