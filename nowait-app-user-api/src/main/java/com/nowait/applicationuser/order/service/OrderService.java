package com.nowait.applicationuser.order.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import com.nowait.applicationuser.order.dto.CartItemDto;
import com.nowait.applicationuser.order.dto.OrderCreateRequestDto;
import com.nowait.applicationuser.order.dto.OrderCreateResponseDto;
import com.nowait.applicationuser.order.dto.OrderMenuDto;
import com.nowait.applicationuser.order.dto.OrderResponseDto;
import com.nowait.domaincorerdb.menu.entity.Menu;
import com.nowait.domaincorerdb.menu.repository.MenuRepository;
import com.nowait.domaincorerdb.order.entity.OrderItem;
import com.nowait.domaincorerdb.order.entity.OrderStatus;
import com.nowait.domaincorerdb.order.entity.UserOrder;
import com.nowait.domaincorerdb.order.exception.DuplicateOrderException;
import com.nowait.domaincorerdb.order.exception.OrderItemsEmptyException;
import com.nowait.domaincorerdb.order.exception.OrderParameterEmptyException;
import com.nowait.domaincorerdb.order.repository.OrderItemRepository;
import com.nowait.domaincorerdb.order.repository.OrderRepository;
import com.nowait.domaincorerdb.store.entity.Store;
import com.nowait.domaincorerdb.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {
	private final OrderRepository orderRepository;
	private final StoreRepository storeRepository;
	private final MenuRepository menuRepository;
	private final OrderItemRepository orderItemRepository;
	@Transactional
	public OrderCreateResponseDto createOrder(Long storeId, Long tableId,
		OrderCreateRequestDto orderCreateRequestDto, String sessionId) {
		parameterValidation(storeId, tableId, orderCreateRequestDto);

		// 💡 [중복 주문 방지] signature 생성 및 체크
		String signature = generateOrderSignature(storeId, tableId, orderCreateRequestDto.getItems());
		checkDuplicateOrderSignature(signature);

		// 1. Store 조회
		Store store = storeRepository.findById(storeId)
			.orElseThrow(() -> new IllegalArgumentException("store not found"));

		// 2. UserOrder 생성 및 signature 저장
		UserOrder order = UserOrder.builder()
			.tableId(tableId)
			.store(store)
			.signature(signature) // signature 저장
			.sessionId(sessionId) // sessionId 저장
			.depositorName(orderCreateRequestDto.getDepositorName())
			.status(OrderStatus.WAITING_FOR_PAYMENT)
			.totalPrice(orderCreateRequestDto.getTotalPrice())

			.build();
		UserOrder savedOrder = orderRepository.save(order);

		// 3. 메뉴 ID 리스트 수집 -> Map으로 캐싱
		List<Long> menuIds = orderCreateRequestDto.getItems().stream()
			.map(CartItemDto::getMenuId)
			.toList();

		List<Menu> menus = menuRepository.findAllById(menuIds);
		Map<Long, Menu> menuMap = menus.stream()
			.collect(Collectors.toMap(Menu::getId, Function.identity()));

		// 4. 각 장바구니 항목에 대해 OrderItem 생성 및 저장
		List<OrderItem> orderItems = orderCreateRequestDto.getItems().stream()
			.map(item -> {
				Menu menu = Optional.ofNullable(menuMap.get(item.getMenuId()))
					.orElseThrow(() -> new IllegalArgumentException("menu not found: " + item.getMenuId()));
				return OrderItem.builder()
					.userOrder(savedOrder)
					.menu(menu)
					.quantity(item.getQuantity())
					.build();
			})
			.collect(Collectors.toList());


		orderItemRepository.saveAll(orderItems);

		// 5. 응답 반환
		return OrderCreateResponseDto.fromEntity(savedOrder,orderItems);
	}

	@Transactional(readOnly = true)
	public List<OrderResponseDto> getOrderItemsGroupByOrderId(
		Long storeId, Long tableId, String sessionId) {

		List<UserOrder> userOrders = orderRepository.findByStore_StoreIdAndTableIdAndSessionId(storeId, tableId, sessionId);

		// orderId 기준으로 바로 변환
		return userOrders.stream()
			.map(order -> OrderResponseDto.builder()
				.orderId(order.getId())
				.status(order.getStatus())
				.createdAt(order.getCreatedAt())
				.items(
					order.getOrderItems().stream()
						.map(OrderMenuDto::fromEntity)
						.toList()
				)
				.totalPrice(order.getTotalPrice())
				.build())
			.toList();
	}


	private static void parameterValidation(Long storeId, Long tableId, OrderCreateRequestDto orderCreateRequestDto) {
		if (storeId == null || tableId == null || orderCreateRequestDto == null) {
				throw new OrderParameterEmptyException();
		}
		if (orderCreateRequestDto.getItems() == null || orderCreateRequestDto.getItems().isEmpty()) {
				throw new OrderItemsEmptyException();
		}
		if (orderCreateRequestDto.getDepositorName() == null || orderCreateRequestDto.getDepositorName().trim().isEmpty()) {
				throw new OrderParameterEmptyException();
		}
		if (orderCreateRequestDto.getDepositorName().length() > 20) {
				throw new IllegalArgumentException("Depositor name is too long");
		}
	}
	private String generateOrderSignature(Long storeId, Long tableId, List<CartItemDto> items) {
		String cartString = items.stream()
			.sorted((a, b) -> a.getMenuId().compareTo(b.getMenuId())) // 메뉴 ID 기준 정렬
			.map(item -> item.getMenuId() + ":" + item.getQuantity())
			.collect(Collectors.joining(","));
		String raw = storeId + "-" + tableId + "-" + cartString;
		return DigestUtils.md5DigestAsHex(raw.getBytes());
	}

	private void checkDuplicateOrderSignature(String signature) {
		// 최근 2초 이내 동일 signature 주문이 있는지 검사
		LocalDateTime threshold = LocalDateTime.now().minusSeconds(2);
		boolean exists = orderRepository.existsBySignatureAndCreatedAtAfter(signature, threshold);
		if (exists) {
			throw new DuplicateOrderException();
		}
	}
}
