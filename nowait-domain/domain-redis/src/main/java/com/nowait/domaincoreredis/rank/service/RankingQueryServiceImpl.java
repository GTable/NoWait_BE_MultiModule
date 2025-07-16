package com.nowait.domaincoreredis.rank.service;

import java.util.List;

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
		List<RankingEntry> entries = rankingQueryRepository.findTopStores(RedisKeyUtils.buildCurrentKey(), topN);

		// 2) 사용자 주점이 topN 밖이면 4위까지 + 사용자 주점
		entries = rankingQueryRepository.findTopStoresWithUser(RedisKeyUtils.buildCurrentKey(), userStoreId, topN, entries);

		// 3) 이전 스냅샷에서 등락 정보 조회
		for (int i = 0; i < entries.size(); i++) {
			var entry = entries.get(i);
			Long prevZero = rankingQueryRepository.findPrevRank(RedisKeyUtils.buildPreviousKey(), entry.getStoreId());
			long prevRank = (prevZero == null ? entry.getCurrentRank() : prevZero + 1);
			int delta = (int)(prevRank - entry.getCurrentRank());

			entries.set(i, new RankingEntry(
				entry.getStoreId(),
				entry.getTotalSales(),
				entry.getCurrentRank(),
				delta
			));
		}

		return entries;
	}
}
