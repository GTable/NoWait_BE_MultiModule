package com.nowait.nowaitevent.order.event;

import java.time.Instant;

import com.nowait.domainadminrdb.cancelOrder.entity.CancelReason;

import lombok.Value;

@Value
public class OrderCancelledEvent {
	Long orderId;
	Long storeId;
	String orderSignature;
	CancelReason reason;
	Instant cancelAt;
}
