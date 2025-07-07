package com.nowait.applicationadmin.order.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationadmin.order.dto.OrderResponseDto;
import com.nowait.applicationadmin.order.dto.OrderStatusUpdateResponseDto;
import com.nowait.common.enums.Role;
import com.nowait.domaincorerdb.order.entity.OrderStatus;
import com.nowait.domaincorerdb.order.entity.UserOrder;
import com.nowait.domaincorerdb.order.exception.OrderNotFoundException;
import com.nowait.domaincorerdb.order.exception.OrderUpdateUnauthorizedException;
import com.nowait.domaincorerdb.order.exception.OrderViewUnauthorizedException;
import com.nowait.domaincorerdb.order.repository.OrderRepository;
import com.nowait.domaincorerdb.user.entity.MemberDetails;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domaincorerdb.user.exception.UserNotFoundException;
import com.nowait.domaincorerdb.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {
	private final OrderRepository orderRepository;
	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public List<OrderResponseDto> findAllOrders(Long storeId, MemberDetails memberDetails) {
		User user =  userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
		if (!Role.SUPER_ADMIN.equals(user.getRole()) && !user.getStoreId().equals(storeId)) {
			throw new OrderViewUnauthorizedException();
		}
		return orderRepository.findAllByStore_StoreId(storeId).stream()
			.map(OrderResponseDto::fromEntity)
			.collect(Collectors.toList());
	}

	@Transactional
	public OrderStatusUpdateResponseDto updateOrderStatus(Long orderId, OrderStatus newStatus,
		MemberDetails memberDetails) {
		User user =  userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
		UserOrder userOrder = orderRepository.findById(orderId)
			.orElseThrow(OrderNotFoundException::new);
		if (!Role.SUPER_ADMIN.equals(user.getRole()) && !user.getStoreId().equals(userOrder.getStore().getStoreId())) {
			throw new OrderUpdateUnauthorizedException();
		}
		userOrder.updateStatus(newStatus);
		return OrderStatusUpdateResponseDto.fromEntity(userOrder);
	}
}
