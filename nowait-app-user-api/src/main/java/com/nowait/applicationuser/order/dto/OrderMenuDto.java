package com.nowait.applicationuser.order.dto;

import com.nowait.domaincorerdb.order.entity.OrderItem;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderMenuDto {
	private Long menuId;
	private String menuName;
	private Integer quantity;
	private Integer price;

	public static OrderMenuDto fromEntity(OrderItem orderItem) {
		return OrderMenuDto.builder()
			.menuId(orderItem.getMenu().getId())
			.menuName(orderItem.getMenu().getName())
			.quantity(orderItem.getQuantity())
			.price(orderItem.getMenu().getPrice())
			.build();
	}
}
