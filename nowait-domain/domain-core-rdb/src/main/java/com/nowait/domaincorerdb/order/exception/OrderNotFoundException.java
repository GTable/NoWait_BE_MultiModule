package com.nowait.domaincorerdb.order.exception;

import com.nowait.common.exception.ErrorMessage;

public class OrderNotFoundException extends RuntimeException {
	public OrderNotFoundException() {
		super(ErrorMessage.ORDER_NOT_FOUND.getMessage());
	}
}
