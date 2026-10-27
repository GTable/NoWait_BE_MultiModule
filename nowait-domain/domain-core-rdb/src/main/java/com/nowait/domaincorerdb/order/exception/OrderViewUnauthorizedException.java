package com.nowait.domaincorerdb.order.exception;

import com.nowait.common.exception.ErrorMessage;

public class OrderViewUnauthorizedException extends RuntimeException {
	public OrderViewUnauthorizedException() {
		super(ErrorMessage.ORDER_VIEW_UNAUTHORIZED.getMessage());
	}
}
