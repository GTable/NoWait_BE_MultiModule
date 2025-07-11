package com.nowait.domaincorerdb.storepayment.exception;

import com.nowait.common.exception.ErrorMessage;

public class StorePaymentAlreadyExistsException extends RuntimeException {
	public StorePaymentAlreadyExistsException() {
		super(ErrorMessage.STORE_PAYMENT_ALREADY_EXISTS.getMessage());
	}
}
