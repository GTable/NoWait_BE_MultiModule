package com.nowait.domainorder.exception;

import com.nowaiting.common.exception.ErrorMessage;

public class OrderParameterEmptyException extends RuntimeException {
	public OrderParameterEmptyException() {
		super(ErrorMessage.ORDER_PARAMETER_EMPTY.getMessage());
	}
}
