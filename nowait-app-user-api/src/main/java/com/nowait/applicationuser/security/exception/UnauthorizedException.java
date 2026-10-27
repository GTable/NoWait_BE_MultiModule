package com.nowait.applicationuser.security.exception;

import com.nowait.common.exception.ErrorMessage;

public class UnauthorizedException extends RuntimeException {
	private final ErrorMessage errorMessage;

	public UnauthorizedException() {
		super(ErrorMessage.UNAUTHORIZED.getMessage());
		this.errorMessage = ErrorMessage.UNAUTHORIZED;
	}

	public UnauthorizedException(ErrorMessage errorMessage) {
		super(errorMessage.getMessage());
		this.errorMessage = errorMessage;
	}

	public String getCode() {
		return errorMessage.getCode();
	}
}
