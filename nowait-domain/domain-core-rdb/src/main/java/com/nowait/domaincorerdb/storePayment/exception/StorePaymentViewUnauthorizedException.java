package com.nowait.domaincorerdb.storePayment.exception;

import com.nowait.common.exception.ErrorMessage;

public class StorePaymentViewUnauthorizedException extends RuntimeException {
	public StorePaymentViewUnauthorizedException() {
		super(ErrorMessage.STORE_PAYMENT_VIEW_UNAUTHORIZED.getMessage());
	}
}
