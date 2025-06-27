package com.example.apiadmin.reservation.exception;

import com.nowaiting.common.exception.ErrorMessage;

public class ReservationNotFoundException extends RuntimeException {
	public ReservationNotFoundException() {
		super(ErrorMessage.NOTFOUND_RESERVATION.getMessage());
	}
}
