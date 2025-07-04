package com.nowait.applicationadmin.order.dto;

import java.time.LocalDateTime;

import com.nowait.domaincorerdb.order.entity.OrderStatus;
import com.nowait.domaincorerdb.order.entity.UserOrder;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderResponseDto {
	private Long id;
	private Long tableId;
	private String depositorName;
	private Integer totalPrice;
	private OrderStatus status;
	private LocalDateTime createdAt;

	public static OrderResponseDto fromEntity(UserOrder userOrder) {
		return OrderResponseDto.builder()
			.id(userOrder.getId())
			.tableId(userOrder.getTableId())
			.depositorName(userOrder.getDepositorName())
			.totalPrice(userOrder.getTotalPrice())
			.status(userOrder.getStatus())
			.createdAt(userOrder.getCreatedAt())
			.build();
	}
}
