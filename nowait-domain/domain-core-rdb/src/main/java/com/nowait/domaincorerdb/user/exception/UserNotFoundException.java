package com.nowait.domaincorerdb.user.exception;

import com.nowait.common.exception.ErrorMessage;

public class UserNotFoundException extends RuntimeException {
	public UserNotFoundException() {
		super(ErrorMessage.NOTFOUND_USER.getMessage());
	}
}
