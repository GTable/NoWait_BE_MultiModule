package com.nowait.domaincorerdb.reservation.exception;

import com.nowait.common.enums.ReservationStatus;
import com.nowait.common.exception.ErrorMessage;

public class UnsupportedReservationStatusException extends RuntimeException {
	public UnsupportedReservationStatusException(ReservationStatus status) {
		super(ErrorMessage.UNSUPPORTED_RESERVATION_STATUS.format(status));
	}
}
