package com.nowait.domaincorerdb.storePayment.exception;

import com.nowait.common.exception.ErrorMessage;

public class StorePaymentNotFoundException extends RuntimeException {
	public StorePaymentNotFoundException(String message) {
		super(ErrorMessage.STORE_PAYMENT_PARAMETER_EMPTY.getMessage());
	}
}
