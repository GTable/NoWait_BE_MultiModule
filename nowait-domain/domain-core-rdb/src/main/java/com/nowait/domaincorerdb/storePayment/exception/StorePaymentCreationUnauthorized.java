package com.nowait.domaincorerdb.storePayment.exception;

import com.nowait.common.exception.ErrorMessage;

public class StorePaymentCreationUnauthorized extends RuntimeException {

	public StorePaymentCreationUnauthorized() {
		super(ErrorMessage.STORE_PAYMENT_CREATION_UNAUTHORIZED.getMessage());
	}
}
