package com.nowait.domaincorerdb.reservation.exception;

import com.nowait.common.enums.ReservationStatus;
import com.nowait.common.exception.ErrorMessage;

public class InvalidReservationStatusTransitionException extends RuntimeException {
	public InvalidReservationStatusTransitionException(ReservationStatus current, ReservationStatus target) {
		super(ErrorMessage.INVALID_RESERVATION_STATUS_TRANSITION.format(current, target));
	}
}
