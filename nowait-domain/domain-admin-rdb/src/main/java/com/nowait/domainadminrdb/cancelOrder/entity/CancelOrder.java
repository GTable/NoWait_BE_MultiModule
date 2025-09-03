package com.nowait.domainadminrdb.cancelOrder.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cancel_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CancelOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long orderId;

	@Column(name = "store_id", nullable = false)
	private Long storeId;

	@Column(nullable = false, length = 256)
	private String orderSignature;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private CancelReason reason;

	@Column(nullable = false)
	private Instant cancelAt;

	@PrePersist
	void prePersist() {
		if (cancelAt == null) cancelAt = Instant.now();
	}
}
