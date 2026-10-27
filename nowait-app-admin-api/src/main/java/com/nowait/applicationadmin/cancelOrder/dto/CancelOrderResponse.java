package com.nowait.applicationadmin.cancelOrder.dto;

import java.time.Instant;

import com.nowait.domainadminrdb.cancelOrder.entity.CancelOrder;
import com.nowait.domainadminrdb.cancelOrder.entity.CancelReason;

import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
public class CancelOrderResponse {

	private Long id;
	private Long orderId;
	private String orderSignature;
	private CancelReason reason;
	private Instant cancelAt;

	public CancelOrder fromEntity() {
		return CancelOrder.builder()
			.id(id)
			.orderId(orderId)
			.orderSignature(orderSignature)
			.reason(reason)
			.cancelAt(cancelAt)
			.build();
	}
}
