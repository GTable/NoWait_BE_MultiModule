package com.nowait.domaincorerdb.storepayment.exception;

import com.nowait.common.exception.ErrorMessage;

public class StorePaymentDeleteUnauthorizedException extends RuntimeException{
	public StorePaymentDeleteUnauthorizedException() {
		super(ErrorMessage.STORE_PAYMENT_DELETE_UNAUTHORIZED.getMessage());
	}
}
