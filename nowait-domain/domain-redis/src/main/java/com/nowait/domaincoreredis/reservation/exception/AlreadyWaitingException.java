package com.nowait.domaincoreredis.reservation.exception;

import com.nowait.common.exception.ErrorMessage;

public class AlreadyWaitingException extends RuntimeException {
	public AlreadyWaitingException() {
		super(ErrorMessage.DUPLICATE_RESERVATION.getMessage());
	}
}
