package com.nowait.domaincoreredis.reservation.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import com.nowait.domaincoreredis.common.util.RedisKeyUtils;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class WaitingRedisRepository {

	private final StringRedisTemplate redisTemplate;

	// 대기열 전체 인원수 조회
	public List<ZSetOperations.TypedTuple<String>> getAllWaitingWithScore(Long storeId) {
		String queueKey = RedisKeyUtils.buildWaitingKeyPrefix() + storeId;
		Set<ZSetOperations.TypedTuple<String>> waitingSet = redisTemplate.opsForZSet().rangeWithScores(queueKey, 0, -1);
		return waitingSet == null ? List.of() : new ArrayList<>(waitingSet);
	}

	public List<String> getAllWaitingUserIds(Long storeId) {
		String queueKey = RedisKeyUtils.buildWaitingKeyPrefix() + storeId;
		// 0부터 -1까지: 전체 범위
		Set<String> userIds = redisTemplate.opsForZSet().range(queueKey, 0, -1);
		return userIds == null ? List.of() : new ArrayList<>(userIds);
	}

	// 상태값 저장 및 변경
	public void setWaitingStatus(Long storeId, String userId, String status) {
		String statusKey = RedisKeyUtils.buildWaitingStatusKeyPrefix() + storeId;
		redisTemplate.opsForHash().put(statusKey, userId, status);
	}

	// 상태값 조회
	public String getWaitingStatus(Long storeId, String userId) {
		String statusKey = RedisKeyUtils.buildWaitingStatusKeyPrefix() + storeId;
		Object value = redisTemplate.opsForHash().get(statusKey, userId);
		return value == null ? null : value.toString();
	}

	// partySize 조회
	public Integer getWaitingPartySize(Long storeId, String userId) {
		String partyKey = RedisKeyUtils.buildWaitingPartySizeKeyPrefix() + storeId;
		Object value = redisTemplate.opsForHash().get(partyKey, userId);
		return value == null ? null : Integer.valueOf(value.toString());
	}

	// userId → reservationNumber 조회
	public String getReservationId(Long storeId, String userId) {
		String status = getWaitingStatus(storeId, userId);
		if (!"WAITING".equals(status) && !"CALLING".equals(status)) {
			// 이미 종료된 대기라면, 예약 번호도 없던 것처럼 취급
			return null;
		}
		String numberMapKey = RedisKeyUtils.buildReservationNumberKey(storeId);
		Object val = redisTemplate.opsForHash().get(numberMapKey, userId);

		return val != null ? val.toString() : null;
	}

	// reservationNumber → userId 조회
	public String getUserIdByReservationNumber(Long storeId, String reservationNumber) {
		String userMapKey = RedisKeyUtils.buildReservationUserKey(storeId);
		Object val = redisTemplate.opsForHash().get(userMapKey, reservationNumber);
		return val == null ? null : val.toString();
	}

	public void deleteWaiting(Long storeId, String userId) {
		String numberMapKey = RedisKeyUtils.buildReservationNumberKey(storeId);
		String userMapKey   = RedisKeyUtils.buildReservationUserKey(storeId);

		Object reservationNumber = redisTemplate.opsForHash().get(numberMapKey, userId);

		// userId → reservationNumber 삭제
		redisTemplate.opsForHash().delete(numberMapKey, userId);

		// reservationNumber → userId 삭제
		if (reservationNumber != null) {
			redisTemplate.opsForHash().delete(userMapKey, reservationNumber);
		}

		String statusKey = RedisKeyUtils.buildWaitingStatusKeyPrefix() + storeId;
		redisTemplate.opsForHash().delete(statusKey, userId);

		String queueKey = RedisKeyUtils.buildWaitingKeyPrefix() + storeId;
		redisTemplate.opsForZSet().remove(queueKey, userId);

		String partyKey = RedisKeyUtils.buildWaitingPartySizeKeyPrefix() + storeId;
		redisTemplate.opsForHash().delete(partyKey, userId);

		String calledAtKey = RedisKeyUtils.buildWaitingCalledAtKeyPrefix() + storeId;
		redisTemplate.opsForHash().delete(calledAtKey, userId);
	}

	// 호출 시각 기록
	public void setWaitingCalledAt(Long storeId, String userId, long timestamp) {
		String key = RedisKeyUtils.buildWaitingCalledAtKeyPrefix() + storeId;
		redisTemplate.opsForHash().put(key, userId, String.valueOf(timestamp));

		redisTemplate.expireAt(key, RedisKeyUtils.expireAtNext03());
	}

	public Long getWaitingCalledAt(Long storeId, String userId) {
		String key = RedisKeyUtils.buildWaitingCalledAtKeyPrefix() + storeId;
		Object val = redisTemplate.opsForHash().get(key, userId);
		return val == null ? null : Long.valueOf(val.toString());
	}
}

