package com.nowait.adminsecurity.exception;

import com.nowait.common.exception.ErrorMessage;

public abstract class ResourceNotFoundException extends RuntimeException {
	private final ErrorMessage errorMessage;

	protected ResourceNotFoundException(ErrorMessage errorMessage) {
		super(errorMessage.getMessage());
		this.errorMessage = errorMessage;
	}

	public String getCode() {
		return errorMessage.getCode();
	}
}
