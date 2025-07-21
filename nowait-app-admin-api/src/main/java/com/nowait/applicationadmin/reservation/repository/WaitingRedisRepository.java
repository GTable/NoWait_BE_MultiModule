package com.nowait.applicationadmin.reservation.repository;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.nowait.domaincoreredis.common.util.RedisKeyUtils;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class WaitingRedisRepository {
	private final StringRedisTemplate redisTemplate;

	// 대기열 전체 인원수 조회
	public long getWaitingCountByStoreId(Long storeId) {
		String key = RedisKeyUtils.buildWaitingKeyPrefix() + storeId;
		Long count = redisTemplate.opsForZSet().zCard(key);
		return count == null ? 0 : count;
	}

	// 상태값 저장 및 변경
	public void setWaitingStatus(Long storeId, String userId, String status) {
		String queueKey = RedisKeyUtils.buildWaitingKeyPrefix() + storeId;
		String partyKey = RedisKeyUtils.buildWaitingPartySizeKeyPrefix() + storeId;
		String statusKey = RedisKeyUtils.buildWaitingStatusKeyPrefix() + storeId;
		redisTemplate.opsForHash().put(statusKey, userId, status);
		// WAITING -> CALLING 으로 변경 시 TTL 12h에서 10m로 변경
		if ("CALLING".equals(status)) {
			redisTemplate.expire(queueKey, Duration.ofMinutes(10));
			redisTemplate.expire(partyKey, Duration.ofMinutes(10));
			redisTemplate.expire(statusKey, Duration.ofMinutes(10));
		}
	}

	// 상태값 조회
	public String getWaitingStatus(Long storeId, String userId) {
		String statusKey = RedisKeyUtils.buildWaitingStatusKeyPrefix() + storeId;
		Object value = redisTemplate.opsForHash().get(statusKey, userId);
		return value == null ? null : value.toString();
	}
}

