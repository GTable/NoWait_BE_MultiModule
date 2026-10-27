package com.nowait.applicationadmin.order.dto;

import com.nowait.domaincorerdb.order.entity.OrderStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OrderStatusUpdateRequestDto {
	@Schema(description = "주문 상태", example = "WAITING_FOR_PAYMENT", allowableValues = {"WAITING_FOR_PAYMENT", "COOKING", "COOKED"})
	@NotNull(message = "주문상태는 필수입니다")
	private final OrderStatus orderStatus;
}
