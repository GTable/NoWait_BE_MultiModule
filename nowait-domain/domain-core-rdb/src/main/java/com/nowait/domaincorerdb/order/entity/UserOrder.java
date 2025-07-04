package com.nowait.domaincorerdb.order.entity;

import java.util.ArrayList;
import java.util.List;

import com.nowait.domaincorerdb.base.entity.BaseTimeEntity;
import com.nowait.domaincorerdb.store.entity.Store;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserOrder extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long tableId;

	@Column(length = 64)
	private String signature;

	// Store와 연관관계 (N:1)
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "store_id")
	private Store store;

	@OneToMany(mappedBy = "userOrder", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<OrderItem> orderItems = new ArrayList<>();

	private String sessionId;
	@Column(length = 10) // 예약자 이름 길이 제한
	private String depositorName;

	@Builder.Default
	@Enumerated(value = EnumType.STRING)
	private OrderStatus status = OrderStatus.WAITING_FOR_PAYMENT;

	@Column(nullable = false)
	private Integer totalPrice;

	public void updateStatus(OrderStatus newStatus) {
		this.status = newStatus;
	}

}
