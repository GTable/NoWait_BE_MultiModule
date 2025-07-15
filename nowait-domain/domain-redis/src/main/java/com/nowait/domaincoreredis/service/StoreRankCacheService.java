package com.nowait.domaincoreredis.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class StoreRankCacheService {

	private RedisTemplate<String, String> redisTemplate;
}
