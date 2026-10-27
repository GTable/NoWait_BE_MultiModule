package com.nowait.domaincorerdb.order.exception;

import com.nowait.common.exception.ErrorMessage;
import com.nowait.domaincorerdb.order.entity.OrderStatus;

public class InvalidOrderStatusTransitionException extends RuntimeException {
	public InvalidOrderStatusTransitionException(OrderStatus current, OrderStatus target) {
		super(ErrorMessage.INVALID_ORDER_STATUS_TRANSITION.format(current, target));
	}
}
