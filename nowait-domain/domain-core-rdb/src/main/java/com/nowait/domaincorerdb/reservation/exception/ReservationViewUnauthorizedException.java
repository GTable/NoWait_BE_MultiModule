package com.nowait.domaincorerdb.reservation.exception;

import com.nowait.common.exception.ErrorMessage;

public class ReservationViewUnauthorizedException extends RuntimeException {
	public ReservationViewUnauthorizedException() {
		super(ErrorMessage.RESERVATION_VIEW_UNAUTHORIZED.getMessage());
	}
}

