package com.nowait.domaincoreredis.reservation.repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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

	// 대기열에 이미 있으면 아무것도 바꾸지 않는다. 발급 번호가 겹치지 않도록 일일 시퀀스는 복구 행의 번호까지 올린다
	private static final String RESTORE_WAITING_LUA = """
		if redis.call('ZADD', KEYS[1], 'NX', ARGV[2], ARGV[1]) == 0 then
			return 0
		end
		redis.call('HSET', KEYS[2], ARGV[1], ARGV[3])
		redis.call('HSET', KEYS[3], ARGV[1], ARGV[4])
		redis.call('HSET', KEYS[4], ARGV[1], ARGV[5])
		redis.call('HSET', KEYS[5], ARGV[5], ARGV[1])
		if ARGV[6] ~= '' then
			redis.call('HSET', KEYS[6], ARGV[1], ARGV[6])
		end
		redis.call('SADD', KEYS[7], ARGV[7])
		if tonumber(redis.call('GET', KEYS[8]) or '0') < tonumber(ARGV[8]) then
			redis.call('SET', KEYS[8], ARGV[8])
		end
		for i = 1, #KEYS do
			redis.call('PEXPIRE', KEYS[i], ARGV[9])
		end
		return 1
		""";

	/**
	 * DB 의 진행 중 예약을 Redis 대기열에 되살린다. reservationNumber 는 {storeId}-{yyyyMMdd}-{seq} 형식이다.
	 * @return 실제로 복구했으면 true, 이미 대기열에 있으면 false
	 */
	public boolean restoreWaiting(Long storeId, Long userId, long score, int partySize, String status,
		String reservationNumber, Long calledAt) {
		String[] number = reservationNumber.split("-");
		String user = String.valueOf(userId);
		Long restored = redisTemplate.execute(
			new DefaultRedisScript<>(RESTORE_WAITING_LUA, Long.class),
			List.of(
				RedisKeyUtils.buildWaitingKeyPrefix() + storeId,
				RedisKeyUtils.buildWaitingPartySizeKeyPrefix() + storeId,
				RedisKeyUtils.buildWaitingStatusKeyPrefix() + storeId,
				RedisKeyUtils.buildReservationNumberKey(storeId),
				RedisKeyUtils.buildReservationUserKey(storeId),
				RedisKeyUtils.buildWaitingCalledAtKeyPrefix() + storeId,
				RedisKeyUtils.buildUserActiveKey(user),
				RedisKeyUtils.buildReservationSeqKey(storeId) + ":" + number[1]
			),
			user,
			String.valueOf(score),
			String.valueOf(partySize),
			status,
			reservationNumber,
			calledAt == null ? "" : String.valueOf(calledAt),
			storeId + ":" + reservationNumber,
			String.valueOf(Long.parseLong(number[2])),
			String.valueOf(RedisKeyUtils.expireAtNext03().getTime() - System.currentTimeMillis())
		);
		return Long.valueOf(1L).equals(restored);
	}

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

	public Long getWaitingCount(Long storeId) {
		String queueKey = RedisKeyUtils.buildWaitingKeyPrefix() + storeId;
		Long size = redisTemplate.opsForZSet().zCard(queueKey);

		return size != null ? size : 0L;
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

