package com.nowait.domaincoreredis.rank.service;

import java.util.List;

import com.nowait.domaincoreredis.rank.dto.RankingEntry;

public interface RankingQueryService {

	/**
	 * 로그인한 사용자의 주점을 포함하여 상위 topN 순위와 등락 정보를 반환한다.
	 */
	List<RankingEntry> getRankings(Long userStoreId, int topN);
}
