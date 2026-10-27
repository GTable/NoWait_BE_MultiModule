package com.nowait.domainadminrdb.statistic.dto;

import lombok.Getter;

@Getter
public class StoreSales {
	private final Long storeId;
	private final Integer totalSales;

	public StoreSales(Long storeId, Integer totalSales) {
		this.storeId = storeId;
		this.totalSales = totalSales;
	}
}
