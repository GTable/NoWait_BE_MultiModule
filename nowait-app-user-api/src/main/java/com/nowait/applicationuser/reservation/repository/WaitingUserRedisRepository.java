package com.nowait.applicationuser.reservation.repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.nowait.domaincoreredis.common.util.RedisKeyUtils;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class WaitingUserRedisRepository {
	private final StringRedisTemplate redisTemplate;

	// 중복 등록 방지: 이미 있으면 추가X
	// 특정 주점에 대한 예약 등록
	public boolean addToWaitingQueue(Long storeId, String userId, Integer partySize, long timestamp) {
		String queueKey = RedisKeyUtils.buildWaitingKeyPrefix() + storeId;
		String partyKey = RedisKeyUtils.buildWaitingPartySizeKeyPrefix() + storeId;
		String statusKey = RedisKeyUtils.buildWaitingStatusKeyPrefix() + storeId;

		Boolean added = redisTemplate.opsForZSet().addIfAbsent(queueKey, userId, timestamp);
		if (Boolean.TRUE.equals(added)) {
			redisTemplate.opsForHash().put(partyKey, userId, partySize.toString());
			// TTL 12시간(43200초) 설정
			redisTemplate.expire(queueKey, Duration.ofHours(12));
			redisTemplate.expire(partyKey, Duration.ofHours(12));
			redisTemplate.expire(statusKey, Duration.ofHours(12));
		}
		return Boolean.TRUE.equals(added);
	}
	// 예약한 사람이 등록한 동반인원(partySize) 조회
	public Integer getPartySize(Long storeId, String userId) {
		String partyKey = RedisKeyUtils.buildWaitingPartySizeKeyPrefix() + storeId;
		Object value = redisTemplate.opsForHash().get(partyKey, userId);
		return Integer.valueOf(value.toString());
	}
	// 예약자 대기순위 조회
	public Long getRank(Long storeId, String userId) {
		String key = RedisKeyUtils.buildWaitingKeyPrefix() + storeId;
		return redisTemplate.opsForZSet().rank(key, userId);
	}
	// 예약 취소
	public boolean removeWaiting(Long storeId, String userId) {
		String key = RedisKeyUtils.buildWaitingKeyPrefix() + storeId;
		String partyKey = RedisKeyUtils.buildWaitingPartySizeKeyPrefix() + storeId;
		redisTemplate.opsForZSet().remove(key, userId);
		redisTemplate.opsForHash().delete(partyKey, userId);
		return true;
	}
	// 예약 등록 시간
	public Long getWaitingTimestamp(Long storeId, String userId) {
		String key = RedisKeyUtils.buildWaitingKeyPrefix() + storeId;
		Double score = redisTemplate.opsForZSet().score(key, userId);
		return score == null ? null : score.longValue();
	}

	// 사용자가 대기중인 전체 매장 목록 조회
	public List<Long> getUserWaitingStoreIds(String userId) {
		// key pattern으로 모든 매장 대기열 조회 (keys: waiting:*)
		Set<String> keys = redisTemplate.keys(RedisKeyUtils.buildWaitingKeyPrefix() + "*");
		if (keys == null) return List.of();

		List<Long> result = new ArrayList<>();
		for (String key : keys) {
			// ZSet만 필터링
			String type = redisTemplate.type(key).code();
			if (!"zset".equals(type)) {
				continue; // hash 등은 스킵!
			}
			// waiting:{storeId}만 추출 (waiting:party:7 등은 통과 안 됨)
			Long storeId = Long.valueOf(key.substring(key.lastIndexOf(":") + 1));
			if (redisTemplate.opsForZSet().rank(key, userId) != null) {
				result.add(storeId);
			}
		}
		return result;
	}
	// 상태값 조회
	public String getWaitingStatus(Long storeId, String userId) {
		String statusKey = RedisKeyUtils.buildWaitingStatusKeyPrefix() + storeId;
		Object value = redisTemplate.opsForHash().get(statusKey, userId);
		return value == null ? null : value.toString();
	}

}


