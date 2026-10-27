package com.nowait.domaincorerdb.reservation.exception;

import com.nowait.common.exception.ErrorMessage;

public class ReservationAddUnauthorizedException extends RuntimeException {
	public ReservationAddUnauthorizedException() {
		super(ErrorMessage.RESERVATION_ADD_UNAUTHORIZED.getMessage());
	}
}

