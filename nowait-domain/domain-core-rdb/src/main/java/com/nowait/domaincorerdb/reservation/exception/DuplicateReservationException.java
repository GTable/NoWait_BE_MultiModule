package com.nowait.domaincorerdb.reservation.exception;

import com.nowait.common.exception.ErrorMessage;

public class DuplicateReservationException extends RuntimeException {
	public DuplicateReservationException() {
		super(ErrorMessage.DUPLICATE_RESERVATION.getMessage());
	}
}

