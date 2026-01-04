package com.nowait.applicationuser.reservation.repository;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import com.nowait.applicationuser.reservation.dto.WaitingSnapshot;
import com.nowait.domaincoreredis.common.util.RedisKeyUtils;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class WaitingUserRedisRepository {
	private final StringRedisTemplate redisTemplate;
	private static final String ADD_WAITING_LUA = """
  		-- KEYS
		  -- 1: queueKey
		  -- 2: partyKey
		  -- 3: statusKey
		  -- 4: numberMapKey
		  -- 5: dailySeqKey
		  
		  -- ARGV
		  -- 1: userId
		  -- 2: timestamp (ms)
		  -- 3: partySize
		  -- 4: today (YYYYMMDD)
		  -- 5: storeId
		  -- 6: ttlMillis
		  
		  -- 1) 큐 등록 (중복 방지)
		  local added = redis.call('ZADD', KEYS[1], 'NX', ARGV[2], ARGV[1])
		  if added == 0 then
		    local rid = redis.call('HGET', KEYS[4], ARGV[1])
		    return {0, rid}
		  end
		  
		  -- 2) 일일 시퀀스
		  local seq = redis.call('INCR', KEYS[5])
		  local seqStr = string.format('%04d', seq)
		  local reservationId = ARGV[5] .. '-' .. ARGV[4] .. '-' .. seqStr
		  
		  -- 3) 메타 저장
		  redis.call('HSET', KEYS[4], ARGV[1], reservationId)
		  redis.call('HSET', KEYS[2], ARGV[1], ARGV[3])
		  redis.call('HSET', KEYS[3], ARGV[1], 'WAITING')
		  
		  -- 4) TTL은 최초 1회만
		  if redis.call('PTTL', KEYS[1]) < 0 then
		    for i = 1, #KEYS do
		      redis.call('PEXPIRE', KEYS[i], ARGV[6])
		    end
		  end
		  
		  return {1, reservationId}
		""";

	// 중복 등록 방지: 이미 있으면 추가X
	// 특정 주점에 대한 예약 등록
	public String addToWaitingQueue(Long storeId, String userId, Integer partySize, long timestamp) {
		String queueKey = RedisKeyUtils.buildWaitingKeyPrefix() + storeId;
		String partyKey = RedisKeyUtils.buildWaitingPartySizeKeyPrefix() + storeId;
		String statusKey = RedisKeyUtils.buildWaitingStatusKeyPrefix() + storeId;
		String seqKey = RedisKeyUtils.buildReservationSeqKey(storeId);
		String numberMapKey = RedisKeyUtils.buildReservationNumberKey(storeId);
		String userMapKey = RedisKeyUtils.buildReservationUserKey(storeId);

		Boolean added = redisTemplate.opsForZSet().addIfAbsent(queueKey, userId, timestamp);
		String reservationId;

		if (Boolean.TRUE.equals(added)) {

			reservationId = GenerateReservationNumber(seqKey, storeId);

			// 5) Hash에 저장
			redisTemplate.opsForHash().put(numberMapKey, userId, reservationId);
			redisTemplate.opsForHash().put(userMapKey, reservationId, userId);
			// 6) 기존 partySize, status, TTL 설정
			redisTemplate.opsForHash().put(partyKey, userId, partySize.toString());
			redisTemplate.opsForHash().put(statusKey, userId, "WAITING");

			Duration ttl = calculateTTLUntilNext03AM();

			redisTemplate.expire(queueKey, ttl);
			redisTemplate.expire(partyKey, ttl);
			redisTemplate.expire(statusKey, ttl);
			redisTemplate.expire(seqKey, ttl);
			redisTemplate.expire(numberMapKey, ttl);
		} else {
			Object stored = redisTemplate.opsForHash().get(numberMapKey, userId);
			reservationId = stored != null ? stored.toString() : null;
		}

		return reservationId;
	}

	// 루아 스크립트 사용
	public WaitingSnapshot addToWaitingQueueLua(
		Long storeId,
		String userId,
		Integer partySize,
		long ts,
		Duration ttl
	) {
		String queueKey = RedisKeyUtils.buildWaitingKeyPrefix() + storeId;
		String partyKey = RedisKeyUtils.buildWaitingPartySizeKeyPrefix() + storeId;
		String statusKey = RedisKeyUtils.buildWaitingStatusKeyPrefix() + storeId;
		String numberMapKey = RedisKeyUtils.buildReservationNumberKey(storeId);

		String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
		String dailySeqKey = RedisKeyUtils.buildReservationSeqKey(storeId) + ":" + today;

		List<String> keys = List.of(
			queueKey,
			partyKey,
			statusKey,
			numberMapKey,
			dailySeqKey
		);

		Object result = redisTemplate.execute(
			new DefaultRedisScript<>(ADD_WAITING_LUA, List.class),
			keys,
			userId,
			String.valueOf(ts),
			String.valueOf(partySize),
			today,
			String.valueOf(storeId),
			String.valueOf(ttl.toMillis())
		);

		if (result == null) return null;

		@SuppressWarnings("unchecked")
		List<Object> response = (List<Object>) result;
		if (response.size() < 2) return null;

		Long added = response.get(0) instanceof Long l ? l : Long.parseLong(String.valueOf(response.get(0)));
		String reservationId = String.valueOf(response.get(1));

		// added == 0이면 중복, 1이면 신규 등록
		// 중복인 경우 rank를 null로 반환하여 구분 가능하게 함
		if (added == 0) {
			Integer existingPartySize = getPartySize(storeId, userId);
			Long existingRank = getRank(storeId, userId);
			return new WaitingSnapshot(existingRank, existingPartySize, reservationId);
		}

		Long actualRank = getRank(storeId, userId);
		return new WaitingSnapshot(actualRank, partySize, reservationId);
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
		String statusKey = RedisKeyUtils.buildWaitingStatusKeyPrefix() + storeId;
		String reservationNumberKey = RedisKeyUtils.buildReservationNumberKey(storeId);

		redisTemplate.opsForZSet().remove(key, userId);
		redisTemplate.opsForHash().delete(partyKey, userId);
		redisTemplate.opsForHash().delete(statusKey, userId);
		redisTemplate.opsForHash().delete(reservationNumberKey, userId);

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
		String prefix = RedisKeyUtils.buildWaitingKeyPrefix();
		String pattern = prefix + "*";

		// 1) SCAN 으로 모든 키 수집
		Set<String> matchingKeys = new HashSet<>();
		ScanOptions options = ScanOptions.scanOptions()
			.match(pattern)
			.count(500)   // 한 번에 스캔할 예상 키 개수 힌트 (조정 가능)
			.build();

		// 2) Cursor 로 비차단 스캔
		try (Cursor<byte[]> cursor =
				 redisTemplate.getConnectionFactory()
					 .getConnection()
					 .scan(options)) {
			while (cursor.hasNext()) {
				String key = new String(cursor.next(), StandardCharsets.UTF_8);
				matchingKeys.add(key);
			}
		}

		if (matchingKeys.isEmpty()) {
			return Collections.emptyList();
		}

		// 2) ZSet 타입 키만 필터링
		List<String> zsetKeys = matchingKeys.stream()
			.filter(key -> "zset".equals(redisTemplate.type(key).code()))
			.toList();

		if (zsetKeys.isEmpty()) {
			return Collections.emptyList();
		}

		// 3) 파이프라인으로 zRank 한 번에 조회
		List<Object> pipelineResults = redisTemplate.executePipelined(
			(RedisCallback<Object>)conn -> {
				byte[] uid = redisTemplate.getStringSerializer().serialize(userId);
				for (String key : zsetKeys) {
					byte[] rawKey = redisTemplate.getStringSerializer().serialize(key);
					conn.zRank(rawKey, uid);
				}
				return null;
			}
		);

		// 4) zsetKeys 로 루프를 돌며 결과 매핑
		List<Long> result = new ArrayList<>();
		Iterator<Object> it = pipelineResults.iterator();
		for (String key : zsetKeys) {
			Object rankObj = it.next();   // pipelineResults 개수와 정확히 매칭
			if (rankObj != null) {
				long storeId = Long.parseLong(key.substring(prefix.length()));
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

	// 사용자가 해당 스토어 대기열에 있는지 여부 반환
	public boolean isUserWaiting(Long storeId, String userId) {
		return getRank(storeId, userId) != null;
	}

	// ReservationNumber 조회
	public String getReservationId(Long storeId, String userId) {
		String numberMapKey = RedisKeyUtils.buildReservationNumberKey(storeId);
		Object val = redisTemplate.opsForHash().get(numberMapKey, userId);

		return val != null ? val.toString() : null;
	}

	// 6) TTL 계산: 다음날 03:00 까지 남은 시간
	public Duration calculateTTLUntilNext03AM() {
		// 6-1) Asia/Seoul 기준으로 오늘 자정(내일 00:00) 구하기
		ZoneId zone = ZoneId.of("Asia/Seoul");
		LocalDateTime now = LocalDateTime.now(zone);
		LocalDateTime midnight = now.toLocalDate().plusDays(1).atTime(3, 0);

		// 6-2) TTL 남은 초 계산
		long secondsUntilMidnight = now.until(midnight, ChronoUnit.SECONDS);

		return Duration.ofSeconds(secondsUntilMidnight);
	}

	// 예약 번호 생성
	public String GenerateReservationNumber(String seqKey, Long storeId) {
		// 2) 일일 시퀀스: 날짜별로 초기화하려면
		String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // YYYYMMDD
		String dailySeqKey = seqKey + ":" + today;  // ex. reservation:seq:5:20250728

		// atomic increment
		Long seq = redisTemplate.opsForValue().increment(dailySeqKey, 1);

		// 3) 4자리 0패딩
		String seqStr = String.format("%04d", seq);

		// 4) 최종 ID 조합
		String reservationId = storeId + "-" + today + "-" + seqStr;

		return reservationId;
	}

	public WaitingSnapshot getWaitingSnapshot(Long storeId, String userId) {
		String queueKey = RedisKeyUtils.buildWaitingKeyPrefix() + storeId;
		String partyKey = RedisKeyUtils.buildWaitingPartySizeKeyPrefix() + storeId;
		String numberMapKey = RedisKeyUtils.buildReservationNumberKey(storeId);

		List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) conn -> {
			byte[] qk = redisTemplate.getStringSerializer().serialize(queueKey);
			byte[] pk = redisTemplate.getStringSerializer().serialize(partyKey);
			byte[] nk = redisTemplate.getStringSerializer().serialize(numberMapKey);
			byte[] uid = redisTemplate.getStringSerializer().serialize(userId);

			conn.zRank(qk, uid);
			conn.hGet(pk, uid);
			conn.hGet(nk, uid);
			return null;
		});

		if (results == null || results.size() < 3) {
			return new WaitingSnapshot(null, null, null);
		}

		// 1) rank
		Long rank = (results.get(0) instanceof Long r) ? r : null;

		// 2) partySize
		Integer partySize = null;
		Object psObj = results.get(1);
		if (psObj instanceof String s) {
			partySize = Integer.valueOf(s);
		} else if (psObj instanceof byte[] b) {
			String deserialized = redisTemplate.getStringSerializer().deserialize(b);
			partySize = deserialized != null ? Integer.valueOf(deserialized) : null;
		}

		// 3) reservationId
		String reservationId = null;
		Object ridObj = results.get(2);
		if (ridObj instanceof String s) {
			reservationId = s;
		} else if (ridObj instanceof byte[] b) {
			reservationId = redisTemplate.getStringSerializer().deserialize(b);
		}

		return new WaitingSnapshot(rank, partySize, reservationId);
	}
}


