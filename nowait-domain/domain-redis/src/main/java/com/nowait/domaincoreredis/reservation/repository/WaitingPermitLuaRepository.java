package com.nowait.domaincoreredis.reservation.repository;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.nowait.domaincoreredis.common.util.RedisKeyUtils;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class WaitingPermitLuaRepository {

	private final StringRedisTemplate redis;

	private static final String ACQUIRE_SCRIPT =
		"redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[1]);" +
		"local holding = redis.call('ZCARD', KEYS[1]);" +
		"local active  = redis.call('SCARD', KEYS[2]);" +
		"if (holding + active) >= tonumber(ARGV[3]) then return 0 end;" +
		"redis.call('ZADD', KEYS[1], tonumber(ARGV[1]) + tonumber(ARGV[2]), ARGV[4]);" +
		"return 1;";

	private static final String ACQUIRE_SCRIPT_V2 =
		"""
			-- KEYS[1] = holding zset   (u:{uid}:holding)
			-- KEYS[2] = active set    (u:{uid}:active)
			-- KEYS[3] = lease count   (u:{uid}:lease:cnt)
			
			-- ARGV[1] = nowMs
			-- ARGV[2] = leaseMs
			-- ARGV[3] = limit
			-- ARGV[4] = token
			-- ARGV[5] = ttlMs
			
			-- 1) 만료된 holding 정리 + cnt 보정
			local removed = redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[1])
			if removed and removed > 0 then
			local after = redis.call('DECRBY', KEYS[3], removed)
			if after < 0 then redis.call('SET', KEYS[3], 0) end
			end
			
			-- 2) 정확한 limit 체크 (핵심)
			local active = redis.call('SCARD', KEYS[2])
			local holding = redis.call('ZCARD', KEYS[1])
			
			if (active + holding) >= tonumber(ARGV[3]) then
			return 0
			end
			
			-- 3) lease 카운트 증가
			redis.call('INCR', KEYS[3])
			
			-- 4) holding 추가
			redis.call(
			'ZADD',
			KEYS[1],
			tonumber(ARGV[1]) + tonumber(ARGV[2]),
			ARGV[4]
			)
			
			-- 5) TTL 동기화
			redis.call('PEXPIRE', KEYS[1], ARGV[5])
			redis.call('PEXPIRE', KEYS[2], ARGV[5])
			redis.call('PEXPIRE', KEYS[3], ARGV[5])
			
			return 1
		""";

	private static final String FINALIZE_SCRIPT =
		"redis.call('ZREM', KEYS[1], ARGV[1]);" +
		"redis.call('SADD', KEYS[2], ARGV[2]);" +
		"return 1;";

	private static final String RELEASE_SCRIPT_V2 =
		"""
			local removed = redis.call('ZREM', KEYS[1], ARGV[1])
			if removed == 1 then
			  local cnt = redis.call('DECR', KEYS[2])
			  if cnt < 0 then redis.call('SET', KEYS[2], 0) end
			end
			return removed
		""";

	private static final String REMOVE_ACTIVE_SCRIPT =
		"""
		local removed = redis.call('SREM', KEYS[1], ARGV[1])
		if removed == 1 then
		  local cnt = redis.call('DECR', KEYS[2])
		  if cnt < 0 then redis.call('SET', KEYS[2], 0) end
		end
		return removed
		""";


	public boolean acquireLease(String userId, String token, long nowMs, long leaseMs, int limit, Duration ttlTo3am) {
		final String hk = RedisKeyUtils.buildUserHoldingKey(userId); // u:{uid}:holding
		final String ak = RedisKeyUtils.buildUserActiveKey(userId);  // u:{uid}:active
		final String ck = RedisKeyUtils.buildUserLeaseCountKey(userId); // u:{uid}:lease:cnt

		Long ok = redis.execute((RedisCallback<Long>) conn -> {
			Object res = conn.eval(
				ACQUIRE_SCRIPT_V2.getBytes(StandardCharsets.UTF_8),
				ReturnType.INTEGER,
				3,
				raw(hk), raw(ak), raw(ck),
				raw(Long.toString(nowMs)),
				raw(Long.toString(leaseMs)),
				raw(Integer.toString(limit)),
				raw(token),
				raw(Long.toString(ttlTo3am.toMillis()))
			);
			return (Long) res;
		});
		return ok != null && ok == 1L;
	}

	public void finalizeActive(String userId, String token, String storeId, String reservationId, Duration ttlTo3am) {
		final String hk = RedisKeyUtils.buildUserHoldingKey(userId);
		final String ak = RedisKeyUtils.buildUserActiveKey(userId);
		final String member = storeId + ":" + reservationId;

		redis.execute((RedisCallback<Void>) conn -> {
			conn.eval(
				FINALIZE_SCRIPT.getBytes(StandardCharsets.UTF_8),
				ReturnType.INTEGER,
				2,
				raw(hk), raw(ak),
				raw(token), raw(member)
			);
			conn.pExpire(raw(ak), ttlTo3am.toMillis());
			return null;
		});
	}

	public void releaseLease(String userId, String token) {
		final String hk = RedisKeyUtils.buildUserHoldingKey(userId);
		final String ck = RedisKeyUtils.buildUserLeaseCountKey(userId);

		redis.execute((RedisCallback<Void>) conn -> {
			conn.eval(
				RELEASE_SCRIPT_V2.getBytes(StandardCharsets.UTF_8),
				ReturnType.INTEGER,
				2,
				raw(hk), raw(ck),
				raw(token)
			);
			return null;
		});
	}

	public Set<String> getActiveMembers(String userId) {
		final String ak = RedisKeyUtils.buildUserActiveKey(userId);
		return redis.execute((RedisCallback<Set<String>>) conn -> {
			Set<byte[]> raw = conn.sMembers(raw(ak));
			if (raw == null || raw.isEmpty()) return Collections.emptySet();
			Set<String> out = new HashSet<>(raw.size());
			for (byte[] b : raw) out.add(string(b));
			return out;
		});
	}

	public void removeActiveMember(String userId, String storeId, String reservationId) {
		final String ak = RedisKeyUtils.buildUserActiveKey(userId);
		final String ck = RedisKeyUtils.buildUserLeaseCountKey(userId);
		final String member = storeId + ":" + reservationId;

		redis.execute((RedisCallback<Void>) conn -> {
			conn.eval(
				REMOVE_ACTIVE_SCRIPT.getBytes(StandardCharsets.UTF_8),
				ReturnType.INTEGER,
				2,
				raw(ak), raw(ck),
				raw(member)
			);
			return null;
		});
	}

	private byte[] raw(String s) { return redis.getStringSerializer().serialize(s); }
	private String string(byte[] b) { return redis.getStringSerializer().deserialize(b); }
}
