package com.nowait.domaincorerdb.storePayment.exception;

import com.nowait.common.exception.ErrorMessage;

public class StorePaymentParamEmptyException extends RuntimeException {
	public StorePaymentParamEmptyException() {
		super(ErrorMessage.STORE_PAYMENT_PARAMETER_EMPTY.getMessage());
	}
}
