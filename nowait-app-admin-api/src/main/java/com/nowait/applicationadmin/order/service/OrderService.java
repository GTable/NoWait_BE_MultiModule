package com.nowait.applicationadmin.order.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationadmin.order.dto.OrderResponseDto;
import com.nowait.applicationadmin.order.dto.OrderStatusUpdateResponseDto;
import com.nowait.common.enums.Role;
import com.nowait.domainadminrdb.statistic.dto.OrderSalesSumDetail;
import com.nowait.domainadminrdb.statistic.dto.TopSalesStoresDetail;
import com.nowait.domainadminrdb.statistic.repository.StatisticCustomRepository;
import com.nowait.domaincorerdb.order.entity.OrderStatus;
import com.nowait.domaincorerdb.order.entity.UserOrder;
import com.nowait.domaincorerdb.order.exception.OrderNotFoundException;
import com.nowait.domaincorerdb.order.exception.OrderUpdateUnauthorizedException;
import com.nowait.domaincorerdb.order.exception.OrderViewUnauthorizedException;
import com.nowait.domaincorerdb.order.repository.OrderRepository;
import com.nowait.domaincorerdb.store.exception.StoreNotFoundException;
import com.nowait.domaincorerdb.store.repository.StoreRepository;
import com.nowait.domaincorerdb.user.entity.MemberDetails;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domaincorerdb.user.exception.UserNotFoundException;
import com.nowait.domaincorerdb.user.repository.UserRepository;
import com.nowait.nowaitevent.order.event.CookingCompleteEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {
	private final OrderRepository orderRepository;
	private final StatisticCustomRepository statisticCustomRepository;
	private final UserRepository userRepository;
	private final StoreRepository storeRepository;
	private final ApplicationEventPublisher publisher;

	@Transactional(readOnly = true)
	public List<OrderResponseDto> findAllOrders(Long storeId, MemberDetails memberDetails) {
		User user = userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
		storeRepository.findByStoreIdAndDeletedFalse(storeId).orElseThrow(StoreNotFoundException::new);
		if (!Role.SUPER_ADMIN.equals(user.getRole()) && !user.getStoreId().equals(storeId)) {
			throw new OrderViewUnauthorizedException();
		}

		LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
		LocalDateTime startDateTime = today.atStartOfDay();
		LocalDateTime endDateTime = today.plusDays(1).atStartOfDay();
		return orderRepository.findAllByStore_StoreIdAndCreatedAtBetween(storeId, startDateTime, endDateTime)
			.stream()
			.map(OrderResponseDto::fromEntity)
			.collect(Collectors.toList());
	}

	@Transactional
	public OrderStatusUpdateResponseDto updateOrderStatus(Long orderId, OrderStatus newStatus,
		MemberDetails memberDetails) {
		User user = userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
		UserOrder userOrder = orderRepository.findById(orderId).orElseThrow(OrderNotFoundException::new);
		if (!Role.SUPER_ADMIN.equals(user.getRole()) && !user.getStoreId().equals(userOrder.getStore().getStoreId())) {
			throw new OrderUpdateUnauthorizedException();
		}
		userOrder.updateStatus(newStatus);

		if (OrderStatus.COOKED.equals(newStatus)) {
			List<CookingCompleteEvent.Item> items = userOrder.getOrderItems().stream()
				.map(item -> new CookingCompleteEvent.Item(
					item.getMenu().getId(),
					item.getQuantity()
				))
				.toList();

			publisher.publishEvent(
				new CookingCompleteEvent(
					userOrder.getStore().getStoreId(),
					items
				)
			);
		}

		return OrderStatusUpdateResponseDto.fromEntity(userOrder);
	}

	@Transactional
	public OrderStatusUpdateResponseDto deleteOrder(Long orderId, MemberDetails memberDetails) {
		User user = userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
		UserOrder userOrder = orderRepository.findById(orderId).orElseThrow(OrderNotFoundException::new);

		if (!Role.SUPER_ADMIN.equals(user.getRole()) && !user.getStoreId().equals(userOrder.getStore().getStoreId())) {
			throw new OrderUpdateUnauthorizedException();
		}

		userOrder.deleteOrder();

		return OrderStatusUpdateResponseDto.fromEntity(userOrder);
	}

	@Transactional(readOnly = true)
	public OrderSalesSumDetail getSaleSumByStoreId(MemberDetails memberDetails, LocalDate date) {
		User user = userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
		Long storeId = user.getStoreId();

		if (!Role.SUPER_ADMIN.equals(user.getRole()) && !user.getStoreId().equals(storeId)) {
			throw new OrderViewUnauthorizedException();
		}

		return statisticCustomRepository.findSalesSumByStoreId(storeId, date);
	}

	@Transactional(readOnly = true)
	public List<TopSalesStoresDetail> getTop5StoresBySalesToday(MemberDetails memberDetails) {
		User user = userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
		Long storeId = user.getStoreId();

		if (!Role.SUPER_ADMIN.equals(user.getRole()) && !user.getStoreId().equals(storeId)) {
			throw new OrderViewUnauthorizedException();
		}

		return statisticCustomRepository.getTop4PlusMine(storeId);
	}
}
