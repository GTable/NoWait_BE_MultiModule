package com.nowait.domaincoreredis.common.cache;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.SubscriptionListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;

/**
 * 로컬 캐시(Caffeine) + Redis Pub/Sub 키 무효화 전파
 * - 값은 공유하지 않고 키만 전파한다 (직렬화 불필요, 결과적 일관성)
 * - put 은 의도적으로 제공하지 않는다. 갱신은 invalidate 후 get 으로 재로딩
 * - 키는 메시지에 그대로 싣기 위해 String 으로 제한한다
 */
public abstract class DistributedCachedDao<V> implements MessageListener, SubscriptionListener {

	// XFetch 의 beta. 1 이 논문 기본값이고, 키울수록 더 일찍 갱신한다
	private static final double BETA = 1.0;

	private final LoadingCache<String, V> cache;
	private final Duration ttl;
	private final String channel;
	private final CacheInvalidationPublisher publisher;
	// XFetch 의 delta: 마지막 로딩에 걸린 시간
	// ponytail: 키별 delta 대신 DAO 하나에 값 하나, 키마다 로딩 비용이 크게 다르면 값과 함께 저장
	private volatile long loadNanos;

	protected DistributedCachedDao(String channel, Duration ttl, long maximumSize,
		RedisMessageListenerContainer container, CacheInvalidationPublisher publisher, MeterRegistry meterRegistry) {
		this.cache = Caffeine.newBuilder()
			.expireAfterWrite(ttl)
			.maximumSize(maximumSize)
			.recordStats()
			.build(this::timedLoad);
		this.ttl = ttl;
		this.channel = channel;
		this.publisher = publisher;
		// 히트율: cache_gets_total{cache=<channel>, result=hit|miss}
		CaffeineCacheMetrics.monitor(meterRegistry, cache, channel);
		container.addMessageListener(this, new ChannelTopic(channel));
	}

	protected abstract V load(String key);

	// Probabilistic Early Expiration: 만료가 가까운 요청이 확률적으로 미리 다시 읽어, 만료 순간 미스가 몰리지 않게 한다.
	// refresh 는 비동기라 그동안 기존 값을 돌려주고, 갱신 중 무효화되면 Caffeine 이 갱신 결과를 버린다
	public V get(String key) {
		V value = cache.get(key);
		Optional<Duration> age = cache.policy().expireAfterWrite().flatMap(expiry -> expiry.ageOf(key));
		if (age.isPresent() && shouldRefreshEarly(ttl.minus(age.get()).toNanos(), loadNanos,
			ThreadLocalRandom.current().nextDouble())) {
			cache.refresh(key);
		}
		return value;
	}

	// XFetch (Vattani et al., 2015): 남은 수명 <= delta * beta * -ln(rand) 이면 갱신
	static boolean shouldRefreshEarly(long remainingNanos, long deltaNanos, double rand) {
		return remainingNanos <= -deltaNanos * BETA * Math.log(rand);
	}

	private V timedLoad(String key) {
		long start = System.nanoTime();
		V value = load(key);
		loadNanos = System.nanoTime() - start;
		return value;
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
