package com.nowait.applicationuser.security.exception;

import com.nowait.common.exception.ErrorMessage;

public class RefreshTokenNotFoundException extends ResourceNotFoundException {

	public RefreshTokenNotFoundException() {
		super(ErrorMessage.REFRESH_TOKEN_NOT_FOUND);
	}

}
