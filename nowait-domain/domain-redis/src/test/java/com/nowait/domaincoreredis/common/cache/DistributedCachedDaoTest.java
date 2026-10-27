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
}
