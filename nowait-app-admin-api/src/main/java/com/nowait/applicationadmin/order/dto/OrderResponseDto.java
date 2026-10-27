package com.nowait.applicationadmin.order.dto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.nowait.domaincorerdb.order.entity.OrderItem;
import com.nowait.domaincorerdb.order.entity.OrderStatus;
import com.nowait.domaincorerdb.order.entity.UserOrder;

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
	private HashMap<String, MenuDetail> menuDetails;
	private LocalDateTime createdAt;

	public static OrderResponseDto fromEntity(UserOrder userOrder) {
		HashMap<String, MenuDetail> menuDetails = new LinkedHashMap<>();

		for (OrderItem item : userOrder.getOrderItems()) {
			String adminDisplayName = item.getMenu().getAdminDisplayName();
			String displayName = (adminDisplayName == null || adminDisplayName.isBlank())
				? item.getMenu().getName() // 관리자 표시명이 없거나 공백이면 일반 메뉴명 사용
				: adminDisplayName;

			int quantity = item.getQuantity();
			int price = item.getMenu().getPrice(); // 메뉴 단가

			// 메뉴명이 중복되면 수량만 누적
			// merge 람다함수 활용(해당 key가 있으면 수량 누적, 없으면 새로 생성)
			menuDetails.merge(displayName,
				new MenuDetail(quantity, price),
				(oldVal, newVal) -> new MenuDetail(
					oldVal.getQuantity() + newVal.getQuantity(),
					oldVal.getPrice() // 단가는 동일하다는 가정
				)
			);
		}

		return OrderResponseDto.builder()
			.id(userOrder.getId())
			.tableId(userOrder.getTableId())
			.depositorName(userOrder.getDepositorName())
			.totalPrice(userOrder.getTotalPrice())
			.status(userOrder.getStatus())
			.menuDetails(menuDetails)
			.createdAt(userOrder.getCreatedAt())
			.build();
	}
}

