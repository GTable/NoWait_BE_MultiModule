package com.nowait.domaincorerdb.order.exception;

import com.nowait.common.exception.ErrorMessage;

public class OrderAlreadyCancelledException extends RuntimeException {
	public OrderAlreadyCancelledException() {
		super(ErrorMessage.ORDER_ALREADY_CANCELLED.getMessage());
	}
}
