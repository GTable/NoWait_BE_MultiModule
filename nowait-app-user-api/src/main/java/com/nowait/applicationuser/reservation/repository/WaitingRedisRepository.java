package com.nowait.applicationuser.reservation.repository;


import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import com.nowait.domaincoreredis.common.util.RedisKeyUtils;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class WaitingRedisRepository {
	private final StringRedisTemplate redisTemplate;


	// 중복 등록 방지: 이미 있으면 추가X
	public boolean addToWaitingQueue(Long storeId, String userId, Integer partySize, long timestamp) {
		String queueKey = RedisKeyUtils.buildWaitingKeyPrefix() + storeId;
		String partyKey = RedisKeyUtils.buildWaitingPartySizeKeyPrefix() + storeId;

		Boolean added = redisTemplate.opsForZSet().addIfAbsent(queueKey, userId, timestamp);
		if (Boolean.TRUE.equals(added)) {
			redisTemplate.opsForHash().put(partyKey, userId, partySize.toString());
		}
		return Boolean.TRUE.equals(added);
	}

	public Integer getPartySize(Long storeId, String userId) {
		String partyKey = RedisKeyUtils.buildWaitingPartySizeKeyPrefix() + storeId;
		Object value = redisTemplate.opsForHash().get(partyKey, userId);
		return value == null ? null : Integer.valueOf(value.toString());
	}

	public Long getRank(Long storeId, String userId) {
		String key = RedisKeyUtils.buildWaitingKeyPrefix() + storeId;
		Set<String> members = redisTemplate.opsForZSet().range(key, 0, -1);
		if (members == null) return null;
		int idx = 0;
		for (String m : members) {
			if (m.startsWith(userId + ":")) {
				return (long) idx;
			}
			idx++;
		}
		return null;
	}

	public Long getWaitingCount(Long storeId) {
		String key = RedisKeyUtils.buildWaitingKeyPrefix() + storeId;
		return redisTemplate.opsForZSet().zCard(key);
	}

	public void removeWaiting(Long storeId, String userId) {
		String key = RedisKeyUtils.buildWaitingKeyPrefix() + storeId;
		redisTemplate.opsForZSet().remove(key, userId);
	}
}


