package com.nowait.applicationuser.order.dto;

import java.time.LocalDateTime;
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
public class OrderResponseDto {
	private Long orderId;
	private OrderStatus status;
	private LocalDateTime createdAt;
	private List<OrderMenuDto> items;
	private Integer totalPrice;
}
