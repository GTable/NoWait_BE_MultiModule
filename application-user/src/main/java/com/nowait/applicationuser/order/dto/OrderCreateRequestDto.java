package com.nowait.applicationuser.order.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class OrderCreateRequestDto {
	private final String depositorName; // 예약자 이름
	private final List<CartItemDto> items; // 장바구니 항목 리스트

}
