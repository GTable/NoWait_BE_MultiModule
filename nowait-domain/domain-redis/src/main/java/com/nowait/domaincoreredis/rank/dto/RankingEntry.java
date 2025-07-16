package com.nowait.domaincoreredis.rank.dto;

import lombok.Getter;

@Getter
public class RankingEntry {
	private final Long storeId;
	private final Integer totalSales;
	private final Long currentRank;
	private final Integer delta;

	public RankingEntry(Long storeId, Integer totalSales, Long currentRank, Integer delta) {
		this.storeId = storeId;
		this.totalSales = totalSales;
		this.currentRank = currentRank;
		this.delta = delta;
	}
}
