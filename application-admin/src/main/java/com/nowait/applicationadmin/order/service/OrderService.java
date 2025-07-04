package com.nowait.applicationadmin.order.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationadmin.order.dto.OrderResponseDto;
import com.nowait.applicationadmin.order.dto.OrderStatusUpdateResponseDto;
import com.nowait.order.entity.OrderStatus;
import com.nowait.order.entity.UserOrder;
import com.nowait.order.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public List<OrderResponseDto> findAllOrders(Long storeId) {
        return orderRepository.findAllByStore_StoreId(storeId).stream()
                .map(OrderResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderStatusUpdateResponseDto updateOrderStatus(Long orderId, OrderStatus newStatus) {
        UserOrder userOrder = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));
        userOrder.updateStatus(newStatus);
        return OrderStatusUpdateResponseDto.fromEntity(userOrder);
    }
}
