package com.nowait.applicationadmin.order.dto;

import com.nowait.order.entity.OrderStatus;
import com.nowait.order.entity.UserOrder;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OrderStatusUpdateResponseDto {
    private final OrderStatus orderStatus;

    public static OrderStatusUpdateResponseDto fromEntity(UserOrder userOrder) {
        return new OrderStatusUpdateResponseDto(userOrder.getStatus());
    }
}
