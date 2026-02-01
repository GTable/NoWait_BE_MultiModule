package com.nowait.domaincoreredis.reservation.repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import com.nowait.domaincoreredis.common.util.RedisKeyUtils;
import com.nowait.domaincoreredis.reservation.exception.AlreadyWaitingException;
import com.nowait.domaincoreredis.reservation.exception.UserWaitingLimitExceededException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@RequiredArgsConstructor
@Slf4j
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
		String userMapKey = RedisKeyUtils.buildReservationUserKey(storeId);

		Object reservationNumber = redisTemplate.opsForHash().get(numberMapKey, userId);

		// userId → reservationNumber 삭제
		redisTemplate.opsForHash().delete(numberMapKey, userId);

		// reservationNumber → userId 삭제
		redisTemplate.opsForHash().delete(userMapKey, reservationNumber);

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

	/**
	 * 웨이팅 대기열 리팩토링 작업중
	 */
	// 대기열 추가
	public void addWaiting(Long storeId, Long userId, LocalDateTime timestamp) {
		String queueKey = RedisKeyUtils.buildWaitingKeyPrefix() + storeId;
		String userListKey = RedisKeyUtils.buildWaitingUserListKeyPrefix() + userId;
		String userWaitingLimitCountKey = RedisKeyUtils.buildUserWaitingLimitCountKey(String.valueOf(userId));

		long score = timestamp
			.atZone(ZoneId.systemDefault())
			.toInstant()
			.toEpochMilli();

		// TODO ttl 설정 필요
		try {
			redisTemplate.opsForZSet().add(queueKey, String.valueOf(userId), score);
			log.info("웨이팅 대기열 추가 - storeId : {}, userId : {}", storeId, userId);

			redisTemplate.opsForZSet().add(userListKey, String.valueOf(storeId), score);
			log.info("유저 웨이팅 목록 추가 - userId : {}, storeId : {}", userId, storeId);
		} catch (Exception e) {
			log.error("Redis 웨이팅 대기열 추가 실패 - storeId : {}, userId : {}, error: {}", storeId, userId, e.getMessage());
			throw e;
		}
	}

	public void removeWaiting(Long storeId, Long userId) {
		String queueKey = RedisKeyUtils.buildWaitingKeyPrefix() + storeId;
		String userListKey = RedisKeyUtils.buildWaitingUserListKeyPrefix() + userId;
		String userWaitingLimitCountKey = RedisKeyUtils.buildUserWaitingLimitCountKey(String.valueOf(userId));

		try {
			redisTemplate.opsForZSet().remove(queueKey, String.valueOf(userId));
			log.info("웨이팅 대기열 제거 - storeId : {}, userId : {}", storeId, userId);

			redisTemplate.opsForZSet().remove(userListKey, String.valueOf(storeId));
			log.info("유저 웨이팅 목록 제거 - userId : {}, storeId : {}", userId, storeId);

			redisTemplate.opsForValue().decrement(userWaitingLimitCountKey, 1);
			log.info("유저 웨이팅 제한 카운트 감소 - userId : {}, currentCount : {}", userId,
				redisTemplate.opsForValue().get(userWaitingLimitCountKey));
		} catch (Exception e) {
			log.error("Redis 웨이팅 대기열 제거 실패 - storeId : {}, userId : {}, error: {}", storeId, userId, e.getMessage());
			throw e;
		}
	}

	// 웨이팅 등록 요청 시 멱등키 검증
	public void idempotentKeyKeyExists(String idempotentKey, String status) {
		Boolean success = redisTemplate.opsForValue()
			.setIfAbsent(
				idempotentKey,
				status,
				Duration.ofSeconds(10)
			);


		// TODO 멱등하지 않은 요청 응답값 검토 필요
		if (Boolean.FALSE.equals(success)) {
			throw new AlreadyWaitingException();
		}
	}

	public void incrementAndCheckWaitingLimit(Long userId, Long maxLimit) {
		String userWaitingLimitCountKey = RedisKeyUtils.buildUserWaitingLimitCountKey(String.valueOf(userId));

		Long current = redisTemplate.opsForValue().increment(userWaitingLimitCountKey, 1);
		log.info("유저 웨이팅 제한 카운트 증가 - userId : {}, currentCount : {}", userId, redisTemplate.opsForValue().get(userWaitingLimitCountKey));

		// TTL 없으면 하루 단위로 묶어야 함 (중요)
		// redisTemplate.expireAt(key, RedisKeyUtils.expireAtNext03());

		if (current != null && current > maxLimit) {
			redisTemplate.opsForValue().decrement(userWaitingLimitCountKey, 1);
			throw new UserWaitingLimitExceededException();
		}
	}

	// 일일 시퀀스 증가 - 예약 번호 전용
	// TODO : 현재 중복 웨이팅 요청에도 시퀀스가 증가하는 문제가 있음 (추후 개선 필요)
	public Long incrementDailySequence(String dailySeqKey) {
		return redisTemplate.opsForValue().increment(dailySeqKey, 1);
	}

	// TODO : 대기 순번 조회 (추후 사용 예정)
	public Long getWaitingRank(Long storeId, Long userId) {
		String queueKey = RedisKeyUtils.buildWaitingKeyPrefix() + storeId;
		return redisTemplate.opsForZSet().rank(queueKey, String.valueOf(userId));
	}

	// 웨이팅 여부 조회
	// TODO: 구현 필요
	public Boolean isWaiting(Long storeId, Long userId) {
		redisTemplate.opsForZSet()
			.score(
				RedisKeyUtils.buildWaitingKeyPrefix() + storeId,
				String.valueOf(userId)
			);

		Boolean isWaiting = true;

		return isWaiting;
	}
}

