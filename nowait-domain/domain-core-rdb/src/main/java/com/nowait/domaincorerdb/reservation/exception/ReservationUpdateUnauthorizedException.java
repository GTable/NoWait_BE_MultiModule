package com.nowait.domaincorerdb.reservation.exception;

import com.nowait.common.exception.ErrorMessage;

public class ReservationUpdateUnauthorizedException extends RuntimeException {
	public ReservationUpdateUnauthorizedException() {
		super(ErrorMessage.RESERVATION_UPDATE_UNAUTHORIZED.getMessage());
	}
}

