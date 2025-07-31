package com.nowait.domaincorerdb.storepayment.exception;

import com.nowait.common.exception.ErrorMessage;

public class StorePaymentNotFoundException extends RuntimeException {
	public StorePaymentNotFoundException() {
		super(ErrorMessage.STORE_PAYMENT_NOT_FOUND.getMessage());
	}
}
