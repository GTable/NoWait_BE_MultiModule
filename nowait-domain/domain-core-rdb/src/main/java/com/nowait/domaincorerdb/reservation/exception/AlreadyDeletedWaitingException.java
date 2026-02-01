package com.nowait.domaincorerdb.reservation.exception;

import com.nowait.common.exception.ErrorMessage;

public class AlreadyDeletedWaitingException extends RuntimeException {
	public AlreadyDeletedWaitingException() {
		super(ErrorMessage.ALREADY_DELETED_RESERVATION.getMessage());
	}
}
