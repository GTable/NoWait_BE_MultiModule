package com.nowait.applicationuser.reservation.exception;

import com.nowait.common.exception.ErrorMessage;

public class ReservationNotFoundException extends RuntimeException {
	public ReservationNotFoundException() {
		super(ErrorMessage.NOTFOUND_RESERVATION.getMessage());
	}
}
