package com.nowait.applicationuser.order.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class OrderCreateRequestDto {
	@NotBlank(message = "주문자 이름은 필수입니다")
	@Size(max = 10, message = "주문자 이름은 10자 이하여야 합니다")
	private final String depositorName; // 예약자 이름
	private final List<CartItemDto> items; // 장바구니 항목 리스트

}
