package com.nowait.domainadminrdb.statistic.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TopSalesStoresDetail {
	private Long storeId;
	private String storeName;
	private Long departmentId;
	private String departmentName;
	private Integer totalSales;
	private Long storeRank;

	public TopSalesStoresDetail(Long storeId, String storeName, Long departmentId, String departmentName, Integer totalSales,
		Long storeRank) {
		this.storeId = storeId;
		this.storeName = storeName;
		this.departmentId = departmentId;
		this.departmentName = departmentName;
		this.totalSales = totalSales;
		this.storeRank = storeRank;
	}
}
