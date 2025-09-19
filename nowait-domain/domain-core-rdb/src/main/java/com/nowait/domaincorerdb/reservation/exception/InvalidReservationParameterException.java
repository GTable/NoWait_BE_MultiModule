package com.nowait.domaincorerdb.reservation.exception;

import com.nowait.common.exception.ErrorMessage;

public class InvalidReservationParameterException extends RuntimeException {
	public InvalidReservationParameterException(String details) {
		super(ErrorMessage.INVALID_RESERVATION_PARAMETER.format(details));
	}
}
