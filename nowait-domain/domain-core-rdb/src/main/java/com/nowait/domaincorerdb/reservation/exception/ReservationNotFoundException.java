package com.nowait.domaincorerdb.reservation.exception;

import com.nowait.common.exception.ErrorMessage;

public class ReservationNotFoundException extends RuntimeException {
	public ReservationNotFoundException() {
		super(ErrorMessage.NOTFOUND_RESERVATION.getMessage());
	}
}

