package com.nowait.domaincorerdb.order.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderSalesSumDetail {
	private Long storeId;
	private Integer todaySalesSum;
	private Integer yesterdaySalesSum;
	private Integer cumulativeSalesBeforeYesterday;

	public OrderSalesSumDetail(Long storeId, Integer todaySalesSum, Integer yesterdaySalesSum,
		Integer cumulativeSalesBeforeYesterday) {
		this.storeId = storeId;
		this.todaySalesSum = todaySalesSum;
		this.yesterdaySalesSum = yesterdaySalesSum;
		this.cumulativeSalesBeforeYesterday = cumulativeSalesBeforeYesterday;
	}
}
