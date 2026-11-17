package com.nowait.applicationuser.order.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nowait.applicationuser.order.dto.CartItemDto;
import com.nowait.applicationuser.order.dto.OrderCreateRequestDto;
import com.nowait.domaincorerdb.menu.entity.Menu;
import com.nowait.domaincorerdb.menu.exception.MenuSoldOutException;
import com.nowait.domaincorerdb.menu.repository.MenuRepository;
import com.nowait.domaincorerdb.order.repository.OrderItemRepository;
import com.nowait.domaincorerdb.order.repository.OrderRepository;
import com.nowait.domaincorerdb.store.entity.Store;
import com.nowait.domaincorerdb.store.repository.StoreRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	@Mock
	OrderRepository orderRepository;
	@Mock
	StoreRepository storeRepository;
	@Mock
	MenuRepository menuRepository;
	@Mock
	OrderItemRepository orderItemRepository;
	@InjectMocks
	OrderService orderService;

	@Test
	void 품절된_메뉴가_있으면_주문을_거절한다() {
		given(storeRepository.findByPublicCodeAndDeletedFalse("code")).willReturn(Optional.of(mock(Store.class)));
		given(menuRepository.findAllById(List.of(1L)))
			.willReturn(List.of(Menu.builder().id(1L).isSoldOut(true).build()));
		OrderCreateRequestDto request = OrderCreateRequestDto.builder()
			.depositorName("손님")
			.items(List.of(CartItemDto.builder().menuId(1L).quantity(1).build()))
			.totalPrice(1000)
			.build();

		assertThatThrownBy(() -> orderService.createOrder("code", 1L, request, "session"))
			.isInstanceOf(MenuSoldOutException.class);
		then(orderItemRepository).shouldHaveNoInteractions();
	}
}
