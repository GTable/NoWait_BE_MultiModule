package com.nowait.domaincoreredis.rank.repository;

import java.util.List;

import com.nowait.domaincoreredis.rank.dto.RankingEntry;

public interface RankingQueryRepository {

	void addToRanking(String key, Long storeId, Integer totalSales);

	Long findPrevRank(String key, Long storeId);

	List<RankingEntry> findTopStores(String key, int topN);

	List<RankingEntry> findTopStoresWithUser(String key, Long userStoreId, int topN, List<RankingEntry> entries);
}
