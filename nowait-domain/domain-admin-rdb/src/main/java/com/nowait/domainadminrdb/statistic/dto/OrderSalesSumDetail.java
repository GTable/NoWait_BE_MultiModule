package com.nowait.domainadminrdb.statistic.dto;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderSalesSumDetail {
	private Long storeId;
	private Integer todaySalesSum;
	private Integer yesterdaySalesSum;
	private Integer cumulativeSalesBeforeYesterday;
	private LocalDate date;

	public OrderSalesSumDetail(Long storeId, Integer todaySalesSum, Integer yesterdaySalesSum,
		Integer cumulativeSalesBeforeYesterday, LocalDate date) {
		this.storeId = storeId;
		this.todaySalesSum = todaySalesSum;
		this.yesterdaySalesSum = yesterdaySalesSum;
		this.cumulativeSalesBeforeYesterday = cumulativeSalesBeforeYesterday;
		this.date = date;
	}

	public boolean isAllZero() {
		return todaySalesSum == 0 && yesterdaySalesSum == 0 && cumulativeSalesBeforeYesterday == 0;
	}
}
