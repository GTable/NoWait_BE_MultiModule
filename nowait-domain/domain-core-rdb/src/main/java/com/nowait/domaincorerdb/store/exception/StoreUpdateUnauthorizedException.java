package com.nowait.domaincorerdb.store.exception;

import com.nowait.common.exception.ErrorMessage;

public class StoreUpdateUnauthorizedException extends RuntimeException {
	public StoreUpdateUnauthorizedException() {
		super(ErrorMessage.STORE_UPDATE_UNAUTHORIZED.getMessage());
	}
}
