package com.nowait.applicationuser.reservation.repository;

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

	private static final String FINALIZE_SCRIPT =
		"redis.call('ZREM', KEYS[1], ARGV[1]);" +
		"redis.call('SADD', KEYS[2], ARGV[2]);" +
		"return 1;";

	public boolean acquireLease(String userId, String token, long nowMs, long leaseMs, int limit, Duration ttlTo3am) {
		final String hk = RedisKeyUtils.buildUserHoldingKey(userId); // u:{uid}:holding
		final String ak = RedisKeyUtils.buildUserActiveKey(userId);  // u:{uid}:active

		Long ok = redis.execute((RedisCallback<Long>) conn -> {
			Object res = conn.eval(
				ACQUIRE_SCRIPT.getBytes(StandardCharsets.UTF_8),
				ReturnType.INTEGER,
				2,
				raw(hk), raw(ak),
				raw(Long.toString(nowMs)),
				raw(Long.toString(leaseMs)),
				raw(Integer.toString(limit)),
				raw(token)
			);
			// TTL 정렬(스크립트 밖에서)
			conn.pExpire(raw(hk), ttlTo3am.toMillis());
			conn.pExpire(raw(ak), ttlTo3am.toMillis());
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
		redis.execute((RedisCallback<Void>) conn -> {
			conn.zRem(raw(hk), raw(token));
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
		final String member = storeId + ":" + reservationId;
		redis.execute((RedisCallback<Void>) conn -> {
			conn.sRem(raw(ak), raw(member));
			return null;
		});
	}

	private byte[] raw(String s) { return redis.getStringSerializer().serialize(s); }
	private String string(byte[] b) { return redis.getStringSerializer().deserialize(b); }
}
