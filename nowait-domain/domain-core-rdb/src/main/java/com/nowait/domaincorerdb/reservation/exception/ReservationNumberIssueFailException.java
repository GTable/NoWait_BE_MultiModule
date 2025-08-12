package com.nowait.domaincorerdb.reservation.exception;

import com.nowait.common.exception.ErrorMessage;

public class ReservationNumberIssueFailException extends RuntimeException {
	public ReservationNumberIssueFailException() {
		super(ErrorMessage.RESERVATION_NUMBER_ISSUE_FAIL.getMessage());
	}
}

