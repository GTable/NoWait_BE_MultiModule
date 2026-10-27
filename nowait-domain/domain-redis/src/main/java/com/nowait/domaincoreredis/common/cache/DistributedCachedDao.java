package com.nowait.domaincoreredis.common.cache;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.SubscriptionListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;

/**
 * 로컬 캐시(Caffeine) + Redis Pub/Sub 키 무효화 전파
 * - 값은 공유하지 않고 키만 전파한다 (직렬화 불필요, 결과적 일관성)
 * - put 은 의도적으로 제공하지 않는다. 갱신은 invalidate 후 get 으로 재로딩
 * - 키는 메시지에 그대로 싣기 위해 String 으로 제한한다
 */
public abstract class DistributedCachedDao<V> implements MessageListener, SubscriptionListener {

	private final Cache<String, V> cache;
	private final String channel;
	private final CacheInvalidationPublisher publisher;

	protected DistributedCachedDao(String channel, Duration ttl, long maximumSize,
		RedisMessageListenerContainer container, CacheInvalidationPublisher publisher, MeterRegistry meterRegistry) {
		this.cache = Caffeine.newBuilder()
			.expireAfterWrite(ttl)
			.maximumSize(maximumSize)
			.recordStats()
			.build();
		this.channel = channel;
		this.publisher = publisher;
		// 히트율: cache_gets_total{cache=<channel>, result=hit|miss}
		CaffeineCacheMetrics.monitor(meterRegistry, cache, channel);
		container.addMessageListener(this, new ChannelTopic(channel));
	}

	protected abstract V load(String key);

	public V get(String key) {
		return cache.get(key, this::load);
	}

	public void invalidate(String key) {
		cache.invalidate(key);
		publisher.invalidate(channel, key);
	}

	public void clearCache() {
		cache.invalidateAll();
		publisher.clear(channel);
	}

	@Override
	public void onMessage(Message message, byte[] pattern) {
		String body = new String(message.getBody(), StandardCharsets.UTF_8);
		if (CacheInvalidationPublisher.CLEAR.equals(body)) {
			cache.invalidateAll();
		} else if (body.startsWith(CacheInvalidationPublisher.INVALIDATE_PREFIX)) {
			cache.invalidate(body.substring(CacheInvalidationPublisher.INVALIDATE_PREFIX.length()));
		}
	}

	// 최초 구독 + 연결 끊김 후 재구독 시 호출된다 → 끊긴 동안 놓친 무효화를 전체 비우기로 수습
	@Override
	public void onChannelSubscribed(byte[] channel, long count) {
		cache.invalidateAll();
	}
}
