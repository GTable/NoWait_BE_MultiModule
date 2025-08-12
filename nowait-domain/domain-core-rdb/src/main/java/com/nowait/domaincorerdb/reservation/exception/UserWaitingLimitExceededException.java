package com.nowait.domaincorerdb.reservation.exception;

import com.nowait.common.exception.ErrorMessage;

public class UserWaitingLimitExceededException extends RuntimeException {
	public UserWaitingLimitExceededException() {
		super(ErrorMessage.USER_WAITING_LIMIT_EXCEEDED.getMessage());
	}
}

