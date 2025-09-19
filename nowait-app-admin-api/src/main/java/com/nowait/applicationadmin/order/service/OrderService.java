package com.nowait.applicationadmin.order.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationadmin.cancelOrder.dto.CancelOrderRequest;
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
import com.nowait.nowaitevent.order.event.OrderCancelledEvent;

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
		User user = getUser(memberDetails);
		storeRepository.findByStoreIdAndDeletedFalse(storeId).orElseThrow(StoreNotFoundException::new);

		validateViewAuthorization(user, storeId);

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
		User user = getUser(memberDetails);
		UserOrder userOrder = orderRepository.findById(orderId).orElseThrow(OrderNotFoundException::new);
		Long storeId = userOrder.getStore().getStoreId();

		validateUpdateAuthorization(user, storeId);

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
	public OrderStatusUpdateResponseDto cancelOrder(Long orderId, CancelOrderRequest cancelOrderRequest, MemberDetails memberDetails) {
		User user = getUser(memberDetails);
		UserOrder userOrder = orderRepository.findById(orderId).orElseThrow(OrderNotFoundException::new);

		validateUpdateAuthorization(user, userOrder.getStore().getStoreId());

		userOrder.cancelOrder();

		publisher.publishEvent(new OrderCancelledEvent(
			userOrder.getId(),
			userOrder.getStore().getStoreId(),
			userOrder.getSignature(),
			cancelOrderRequest.reason(),
			Instant.now()
		));


		return OrderStatusUpdateResponseDto.fromEntity(userOrder);
	}

	@Transactional(readOnly = true)
	public OrderSalesSumDetail getSaleSumByStoreId(MemberDetails memberDetails, LocalDate date) {
		User user = getUser(memberDetails);
		Long storeId = user.getStoreId();

		validateViewAuthorization(user, storeId);

		return statisticCustomRepository.findSalesSumByStoreId(storeId, date);
	}

	// 현재는 사용하지 않음. 향후 관리자 통계 페이지 확장 시 활용 가능
	@Transactional(readOnly = true)
	public List<TopSalesStoresDetail> getTop5StoresBySalesToday(MemberDetails memberDetails) {
		User user = getUser(memberDetails);
		Long storeId = user.getStoreId();

		validateUpdateAuthorization(user, storeId);

		return statisticCustomRepository.getTop4PlusMine(storeId);
	}

	private void validateViewAuthorization(User user, Long storeId) {
		if (!(Role.SUPER_ADMIN.equals(user.getRole())
			  || (Role.MANAGER.equals(user.getRole()) && storeId.equals(user.getStoreId())))) {
			throw new OrderViewUnauthorizedException();
		}
	}

	private void validateUpdateAuthorization(User user, Long storeId) {
		if (!(Role.SUPER_ADMIN.equals(user.getRole())
			  || (Role.MANAGER.equals(user.getRole()) && storeId.equals(user.getStoreId())))) {
			throw new OrderUpdateUnauthorizedException();
		}
	}

	private User getUser(MemberDetails memberDetails) {
		return userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
	}
}
