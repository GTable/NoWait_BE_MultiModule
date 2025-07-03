package com.nowait.applicationadmin.order.dto;

import java.time.LocalDateTime;

import com.nowait.applicationadmin.reservation.dto.ReservationGetResponseDto;
import com.nowait.order.entity.UserOrder;
import com.nowait.order.entity.OrderStatus;
import com.nowait.reservation.entity.Reservation;

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
