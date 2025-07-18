package com.nowait.applicationuser.order.dto;

import java.util.List;

import com.nowait.domaincorerdb.order.entity.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemGroupByStatusResponseDto {
	private OrderStatus status;
	private List<OrderItemListGetResponseDto> items;
}

