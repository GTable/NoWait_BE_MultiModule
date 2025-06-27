package com.nowaiting.user.exception;

import com.nowaiting.common.exception.ErrorMessage;

public class UserNotFoundException extends RuntimeException {
	public UserNotFoundException() {
		super(ErrorMessage.NOTFOUND_USER.getMessage());
	}
}
