package com.nowait.domaincorerdb.store.exception;

import com.nowait.common.exception.ErrorMessage;

public class StoreDeleteUnauthorizedException extends RuntimeException {
	public StoreDeleteUnauthorizedException() {
		super(ErrorMessage.STORE_DELETE_UNAUTHORIZED.getMessage());
	}
}
