package com.nowait.applicationadmin.order.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.nowait.domaincorerdb.order.entity.OrderItem;
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
	private Map<String, Integer> menuNamesAndQuantities;
	private LocalDateTime createdAt;

	public static OrderResponseDto fromEntity(UserOrder userOrder) {
		Map<String, Integer> menuNamesAndQuantities = userOrder.getOrderItems().stream()
			.collect(Collectors.toMap(
				orderItem -> orderItem.getMenu().getName(),
				OrderItem::getQuantity,
				Integer::sum // 메뉴명이 중복일 때 수량 합침
			));

		return OrderResponseDto.builder()
			.id(userOrder.getId())
			.tableId(userOrder.getTableId())
			.depositorName(userOrder.getDepositorName())
			.totalPrice(userOrder.getTotalPrice())
			.status(userOrder.getStatus())
			.menuNamesAndQuantities(menuNamesAndQuantities)
			.createdAt(userOrder.getCreatedAt())
			.build();
	}
}
