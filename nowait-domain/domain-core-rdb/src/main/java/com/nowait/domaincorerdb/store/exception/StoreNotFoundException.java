package com.nowait.domaincorerdb.store.exception;

import com.nowait.common.exception.ErrorMessage;

public class StoreNotFoundException extends RuntimeException {
	public StoreNotFoundException() {
		super(ErrorMessage.STORE_NOT_FOUND.getMessage());
	}
}
