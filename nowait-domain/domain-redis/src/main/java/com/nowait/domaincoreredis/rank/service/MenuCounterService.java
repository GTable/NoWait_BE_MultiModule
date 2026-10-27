package com.nowait.domaincoreredis.rank.service;

import java.util.Set;

import org.springframework.data.redis.core.ZSetOperations;

public interface MenuCounterService {

	void incrementMenuCounter(Long menuId, Long storeId, int qty);

	Set<ZSetOperations.TypedTuple<String>> getTopMenus(Long storeId, int topN);
}
