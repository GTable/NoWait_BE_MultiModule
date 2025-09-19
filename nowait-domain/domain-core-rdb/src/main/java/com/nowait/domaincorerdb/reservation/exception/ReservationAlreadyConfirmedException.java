package com.nowait.domaincorerdb.reservation.exception;

import com.nowait.common.exception.ErrorMessage;

public class ReservationAlreadyConfirmedException extends RuntimeException {
	public ReservationAlreadyConfirmedException() {
		super(ErrorMessage.RESERVATION_ALREADY_CONFIRMED.getMessage());
	}
}
