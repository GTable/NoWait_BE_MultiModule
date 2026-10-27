package com.nowait.applicationuser.order.dto;

import com.nowait.domaincorerdb.order.entity.OrderItem;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponseDTO {
	private Long menuId;           // 메뉴 ID
	private String menuName;       // 메뉴 이름
	private int quantity;          // 수량

	public static OrderItemResponseDTO fromEntity(OrderItem orderItem) {
		return OrderItemResponseDTO.builder()
			.menuId(orderItem.getMenu().getId())
			.menuName(orderItem.getMenu().getName())
			.quantity(orderItem.getQuantity())
			.build();
	}
}
