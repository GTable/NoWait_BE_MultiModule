package com.nowait.token.exception;

import com.nowait.common.exception.ErrorMessage;

public abstract class BusinessException extends RuntimeException {
	private final ErrorMessage errorMessage;

	protected BusinessException(ErrorMessage errorMessage) {
		super(errorMessage.getMessage());
		this.errorMessage = errorMessage;
	}

	public String getCode() {
		return errorMessage.getCode();
	}
}
