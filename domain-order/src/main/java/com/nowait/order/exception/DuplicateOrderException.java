package com.nowait.order.exception;

import com.nowait.common.exception.ErrorMessage;

public class DuplicateOrderException extends RuntimeException {
	public DuplicateOrderException() {
		super(ErrorMessage.DUPLICATE_ORDER.getMessage());
	}
}
