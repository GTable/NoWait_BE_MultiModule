package com.nowait.domaincoreredis.rank.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import com.nowait.domaincoreredis.common.util.RedisKeyUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuCounterServiceImpl implements MenuCounterService {

	private final String KEY_FMT = RedisKeyUtils.buildMenuKey();
	private final DateTimeFormatter DTF = RedisKeyUtils.buildMenuDateKey();
	private final RedisTemplate<String, String> redis;

	@Override
	public void incrementMenuCounter(Long menuId, Long storeId, int qty) {

		String date = LocalDate.now().format(DTF);
		String key = String.format(KEY_FMT, storeId, date);

		redis.opsForZSet().incrementScore(key, menuId.toString(), qty);

		Long expirationTime = redis.getExpire(key);
		if (expirationTime == null || expirationTime < 0) {
			long secondsUntilMidnight = Duration.between(
				LocalDateTime.now(),
				LocalDate.now().plusDays(1).atStartOfDay()
			).getSeconds();

			redis.expire(key, secondsUntilMidnight, TimeUnit.SECONDS);
		}
	}

	@Override
	public Set<ZSetOperations.TypedTuple<String>> getTopMenus(Long storeId, int topN) {
		String date = LocalDate.now().format(DTF);
		String key = String.format(KEY_FMT, storeId, date);

		return redis.opsForZSet().reverseRangeWithScores(key, 0, topN - 1);
	}
}
