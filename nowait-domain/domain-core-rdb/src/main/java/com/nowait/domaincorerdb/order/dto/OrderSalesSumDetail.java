package com.nowait.domaincorerdb.order.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderSalesSumDetail {
	Long storeId;
	Integer todaySalesSum;
	Integer yesterdaySalesSum;
	Integer previousDaySales;

	public OrderSalesSumDetail(Long storeId, Integer todaySalesSum, Integer yesterdaySalesSum,
		Integer previousDaySales) {
		this.storeId = storeId;
		this.todaySalesSum = todaySalesSum;
		this.yesterdaySalesSum = yesterdaySalesSum;
		this.previousDaySales = previousDaySales;
	}
}
