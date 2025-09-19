package com.nowait.domaincorerdb.reservation.exception;

import com.nowait.common.exception.ErrorMessage;

public class ReservationAlreadyCancelledException extends RuntimeException {
	public ReservationAlreadyCancelledException() {
		super(ErrorMessage.RESERVATION_ALREADY_CANCELLED.getMessage());
	}
}
