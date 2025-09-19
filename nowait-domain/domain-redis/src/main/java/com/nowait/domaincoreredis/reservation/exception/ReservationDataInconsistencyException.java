package com.nowait.domaincoreredis.reservation.exception;

import com.nowait.common.exception.ErrorMessage;

public class ReservationDataInconsistencyException extends RuntimeException {
	public ReservationDataInconsistencyException(String details) {
		super(ErrorMessage.RESERVATION_DATA_INCONSISTENCY.format(details));
	}
}

