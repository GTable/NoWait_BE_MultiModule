package com.nowait.applicationadmin.reservation.repository;

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
}

