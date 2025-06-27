package com.nowait.adminsecurity.exception;

import com.nowaiting.common.exception.ErrorMessage;

public class RefreshTokenNotFoundException extends ResourceNotFoundException {

	public RefreshTokenNotFoundException() {
		super(ErrorMessage.REFRESH_TOKEN_NOT_FOUND);
	}

}
