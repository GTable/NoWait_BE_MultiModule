package com.nowait.order.exception;

import com.nowait.common.exception.ErrorMessage;

public class DepositorNameTooLongException extends RuntimeException {
	public DepositorNameTooLongException() {
		super(ErrorMessage.DUPLICATE_ORDER.getMessage());
	}
}
