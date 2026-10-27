package com.nowait.applicationuser.order.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CartItemDto {
	private final Long menuId;    // 메뉴 ID
	private final int quantity;   // 수량
}
