package com.nowait.domaincorerdb.storePayment.exception;

import com.nowait.common.exception.ErrorMessage;

public class StorePaymentUpdateUnauthorizedException extends RuntimeException {
	public StorePaymentUpdateUnauthorizedException() {
		super(ErrorMessage.STORE_PAYMENT_UPDATE_UNAUTHORIZED.getMessage());
	}
}
