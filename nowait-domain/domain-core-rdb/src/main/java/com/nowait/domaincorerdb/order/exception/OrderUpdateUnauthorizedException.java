package com.nowait.domaincorerdb.order.exception;

import com.nowait.common.exception.ErrorMessage;

public class OrderUpdateUnauthorizedException extends RuntimeException {
	public OrderUpdateUnauthorizedException() {
		super(ErrorMessage.ORDER_UPDATE_UNAUTHORIZED.getMessage());
	}
}
