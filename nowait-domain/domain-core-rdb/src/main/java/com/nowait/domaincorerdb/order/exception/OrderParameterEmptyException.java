package com.nowait.domaincorerdb.order.exception;

import com.nowait.common.exception.ErrorMessage;

public class OrderParameterEmptyException extends RuntimeException {
	public OrderParameterEmptyException() {
		super(ErrorMessage.ORDER_PARAMETER_EMPTY.getMessage());
	}
}
