package com.nowait.domainorder.exception;

import com.nowaiting.common.exception.ErrorMessage;

public class DuplicateOrderException extends RuntimeException {
	public DuplicateOrderException() {
		super(ErrorMessage.DUPLICATE_ORDER.getMessage());
	}
}
