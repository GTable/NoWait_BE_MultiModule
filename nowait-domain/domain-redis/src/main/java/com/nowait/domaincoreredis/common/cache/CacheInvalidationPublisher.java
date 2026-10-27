package com.nowait.domaincoreredis.common.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 캐시 무효화 메시지 발행 (INVALIDATE-{key}, CLEAR)
 * 구독 측은 {@link DistributedCachedDao}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationPublisher {

	public static final String INVALIDATE_PREFIX = "INVALIDATE-";
	public static final String CLEAR = "CLEAR";

	private final StringRedisTemplate redisTemplate;

	public void invalidate(String channel, String key) {
		publishAfterCommit(channel, INVALIDATE_PREFIX + key);
	}

	public void clear(String channel) {
		publishAfterCommit(channel, CLEAR);
	}

	// 커밋 전에 발행하면 구독 측이 evict 직후 커밋 전 값을 다시 읽어 TTL 동안 캐싱한다
	private void publishAfterCommit(String channel, String message) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			publish(channel, message);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				publish(channel, message);
			}
		});
	}

	private void publish(String channel, String message) {
		try {
			redisTemplate.convertAndSend(channel, message);
		} catch (Exception e) {
			// 발행 실패 시 TTL 만료 전까지 옛 값이 남는다 → redis-cli PUBLISH {channel} CLEAR 로 수동 무효화
			log.error("캐시 무효화 발행 실패 channel={}, message={}", channel, message, e);
		}
	}
}
