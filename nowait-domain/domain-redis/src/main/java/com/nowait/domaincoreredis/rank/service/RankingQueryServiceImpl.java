package com.nowait.domaincoreredis.rank.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.nowait.domaincoreredis.common.util.RedisKeyUtils;
import com.nowait.domaincoreredis.rank.dto.RankingEntry;
import com.nowait.domaincoreredis.rank.repository.RankingQueryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RankingQueryServiceImpl implements RankingQueryService {

	private final RankingQueryRepository rankingQueryRepository;

	@Override
	public List<RankingEntry> getRankings(Long userStoreId, int topN) {
		// 1) 현재 스냅샷에서 상위 topN 주점 조회
		List<RankingEntry> entries = getCurrentRankings(userStoreId, topN);
		// 2) 이전 스냅샷에서 등락 정보 조회
		return calculateRankingDeltas(entries);
	}

	private List<RankingEntry> getCurrentRankings(Long userStoreId, int topN) {
		String currentKey = RedisKeyUtils.buildCurrentKey();
		List<RankingEntry> entries = rankingQueryRepository.findTopStores(currentKey, topN);
		return rankingQueryRepository.findTopStoresWithUser(currentKey, userStoreId, topN, entries);
	}

	private List<RankingEntry> calculateRankingDeltas(List<RankingEntry> entries) {
		String previousKey = RedisKeyUtils.buildPreviousKey();
		return entries.stream()
			.map(entry -> calculateDeltaForEntry(entry, previousKey))
			.collect(Collectors.toList());
	}

	private RankingEntry calculateDeltaForEntry(RankingEntry entry, String previousKey) {
		Long prevZero = rankingQueryRepository.findPrevRank(previousKey, entry.getStoreId());
		long prevRank = (prevZero == null ? entry.getCurrentRank() : prevZero + 1);
		int delta = (int)(prevRank - entry.getCurrentRank());
		return new RankingEntry(
			entry.getStoreId(),
			entry.getTotalSales(),
			entry.getCurrentRank(),
			delta
		);
	}
}
