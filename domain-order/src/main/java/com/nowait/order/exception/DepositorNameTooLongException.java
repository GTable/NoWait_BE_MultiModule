package com.nowait.order.exception;

import com.nowait.common.exception.ErrorMessage;

public class DepositorNameTooLongException extends RuntimeException {
	public DepositorNameTooLongException() {
		super(ErrorMessage.DEPOSITOR_NAME_TOO_LONG.getMessage());
	}
}
