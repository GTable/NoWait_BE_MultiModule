package com.nowait.applicationadmin.cancelOrder.listener;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.nowait.domainadminrdb.cancelOrder.entity.CancelOrder;
import com.nowait.domainadminrdb.cancelOrder.repository.CancelOrderRepository;
import com.nowait.nowaitevent.order.event.OrderCancelledEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCancelledEventListener {

	private final CancelOrderRepository cancelOrderRepository;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void on(OrderCancelledEvent event) {
		try {
			cancelOrderRepository.save(
				CancelOrder.builder()
					.orderId(event.getOrderId())
					.storeId(event.getStoreId())
					.orderSignature(event.getOrderSignature())
					.reason(event.getReason())
					.cancelAt(event.getCancelAt())
					.build()
			);
		} catch (DataIntegrityViolationException e) {
			log.debug("cancel_orders duplicate orderId={}, ignore", event.getOrderId());
		}
	}
}
