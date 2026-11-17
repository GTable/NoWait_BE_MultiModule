package com.nowait.domaincoreredis.common.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class DistributedCachedDaoTest {

	static class CountingDao extends DistributedCachedDao<Integer> {
		int loads;

		CountingDao() {
			super("cache:test", Duration.ofDays(1), 100, new RedisMessageListenerContainer(), null,
				new SimpleMeterRegistry());
		}

		@Override
		protected Integer load(String key) {
			return ++loads;
		}
	}

	private static void receive(CountingDao dao, String body) {
		dao.onMessage(new DefaultMessage("cache:test".getBytes(StandardCharsets.UTF_8),
			body.getBytes(StandardCharsets.UTF_8)), null);
	}

	@Test
	void 무효화_메시지를_받으면_해당_키만_다시_로딩한다() {
		CountingDao dao = new CountingDao();
		assertEquals(1, dao.get("1"));
		assertEquals(2, dao.get("2"));
		assertEquals(1, dao.get("1"));

		receive(dao, "INVALIDATE-1");
		assertEquals(3, dao.get("1"));
		assertEquals(2, dao.get("2"));

		receive(dao, "CLEAR");
		assertEquals(4, dao.get("2"));

		dao.onChannelSubscribed("cache:test".getBytes(StandardCharsets.UTF_8), 1);
		assertEquals(5, dao.get("2"));
	}

	@Test
	void TTL_90퍼센트부터_경과할수록_미리_갱신할_확률이_높아진다() {
		Duration ttl = Duration.ofMinutes(5);
		// 90% 전에는 갱신하지 않는다
		assertFalse(DistributedCachedDao.shouldRefreshEarly(ttl.multipliedBy(89).dividedBy(100), ttl, 0.0));
		// 93% → 30%, 95% → 50%, 98% → 80%: rand 가 확률보다 작을 때만 갱신
		assertTrue(DistributedCachedDao.shouldRefreshEarly(ttl.multipliedBy(93).dividedBy(100), ttl, 0.29));
		assertFalse(DistributedCachedDao.shouldRefreshEarly(ttl.multipliedBy(93).dividedBy(100), ttl, 0.31));
		assertTrue(DistributedCachedDao.shouldRefreshEarly(ttl.multipliedBy(95).dividedBy(100), ttl, 0.49));
		assertFalse(DistributedCachedDao.shouldRefreshEarly(ttl.multipliedBy(95).dividedBy(100), ttl, 0.51));
		assertTrue(DistributedCachedDao.shouldRefreshEarly(ttl.multipliedBy(98).dividedBy(100), ttl, 0.79));
		assertFalse(DistributedCachedDao.shouldRefreshEarly(ttl.multipliedBy(98).dividedBy(100), ttl, 0.81));
	}
}
