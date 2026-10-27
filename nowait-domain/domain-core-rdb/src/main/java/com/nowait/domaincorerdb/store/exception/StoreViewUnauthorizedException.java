package com.nowait.domaincorerdb.store.exception;

import com.nowait.common.exception.ErrorMessage;

public class StoreViewUnauthorizedException extends RuntimeException {
	public StoreViewUnauthorizedException() {
		super(ErrorMessage.STORE_VIEW_UNAUTHORIZED.getMessage());
	}
}
