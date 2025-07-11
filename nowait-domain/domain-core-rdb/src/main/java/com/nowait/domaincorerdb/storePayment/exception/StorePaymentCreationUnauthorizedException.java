package com.nowait.domaincorerdb.storepayment.exception;

import com.nowait.common.exception.ErrorMessage;

public class StorePaymentCreationUnauthorizedException extends RuntimeException {

	public StorePaymentCreationUnauthorizedException() {
		super(ErrorMessage.STORE_PAYMENT_CREATION_UNAUTHORIZED.getMessage());
	}
}
