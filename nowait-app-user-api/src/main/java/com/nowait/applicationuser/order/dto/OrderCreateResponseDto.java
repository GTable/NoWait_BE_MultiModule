package com.nowait.applicationuser.order.dto;

import java.util.List;

import com.nowait.domaincorerdb.order.entity.UserOrder;
import com.nowait.domaincorerdb.order.entity.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class OrderCreateResponseDto {
	private Long orderId;                  // 주문 ID 	// 주문 상태 (예: "주문완료", "배송중" 등)
	private Long storeId;                  // 상점 ID
	private String storeName;              // 상점 이름
	private String sessionId;
	private String depositorName;
	private List<OrderItemResponseDTO> orderItems;  // 주문 항목 목록
	private OrderStatus status;
	private Integer totalPrice;

	public static OrderCreateResponseDto fromEntity(UserOrder order) {
		return OrderCreateResponseDto.builder()
			.orderId(order.getId())
			.storeId(order.getStore().getStoreId())
			.storeName(order.getStore().getName())
			.sessionId(order.getSessionId())
			.depositorName(order.getDepositorName())
			.orderItems(List.of())
			.status(order.getStatus())
			.totalPrice(order.getTotalPrice())
			.build();
	}
}
