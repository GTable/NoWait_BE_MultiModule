package com.nowait.domaincoreredis.rank.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.nowait.domaincoreredis.rank.dto.RankingEntry;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RankingQueryRepositoryImpl implements RankingQueryRepository {

	private final StringRedisTemplate redisTemplate;

	@Override
	public void addToRanking(String key, Long storeId, Integer totalSales) {
		double totalSalesDouble = totalSales != null ? totalSales.doubleValue() : 0.0;
		redisTemplate.opsForZSet().add(key, String.valueOf(storeId), totalSalesDouble);
	}

	@Override
	public Long findPrevRank(String key, Long storeId) {
		return redisTemplate.opsForZSet().reverseRank(key, String.valueOf(storeId));
	}

	@Override
	public List<RankingEntry> findTopStores(String key, int topN) {
		var topTuples = redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, topN - 1);

		List<RankingEntry> entries = topTuples.stream()
			.map(tuple -> new RankingEntry(
				Long.parseLong(tuple.getValue()),
				tuple.getScore().intValue(),
				redisTemplate.opsForZSet().reverseRank(key, tuple.getValue()) + 1,
				0
			))
			.collect(Collectors.toCollection(ArrayList::new));

		return entries;
	}

	@Override
	public List<RankingEntry> findTopStoresWithUser(String key, Long userStoreId, int topN, List<RankingEntry> entries) {
		Long userZero = redisTemplate.opsForZSet().reverseRank(key, userStoreId.toString());

		if (userZero != null && userZero >= topN) {
			List<RankingEntry> top4 = entries.subList(0, topN - 1);
			Integer userSales = redisTemplate.opsForZSet().score(key, userStoreId.toString()).intValue();

			top4.add(new RankingEntry(
				userStoreId,
				userSales,
				userZero + 1,
				0 // 초기 등락 값은 0으로 설정
			));
			entries = top4;
		}
		return entries;
	}
}
