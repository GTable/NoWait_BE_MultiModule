package com.nowait.applicationadmin.statistic.dto;

import lombok.Getter;

@Getter
public class StoreRankingDto {
	private final Long   storeId;
	private final String storeName;
	private final Long   departmentId;
	private final String departmentName;
	private final Integer totalSales;
	private final Long    currentRank;
	private final Integer delta;

	public StoreRankingDto(Long storeId, String storeName, Long departmentId, String departmentName, Integer totalSales,
		Long currentRank, Integer delta) {
		this.storeId = storeId;
		this.storeName = storeName;
		this.departmentId = departmentId;
		this.departmentName = departmentName;
		this.totalSales = totalSales;
		this.currentRank = currentRank;
		this.delta = delta;
	}
}
