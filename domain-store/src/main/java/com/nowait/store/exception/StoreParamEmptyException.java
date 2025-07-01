package com.nowait.store.exception;

import com.nowait.common.exception.ErrorMessage;

public class StoreParamEmptyException extends RuntimeException {
	public StoreParamEmptyException() {
		super(ErrorMessage.STORE_PARAMETER_EMPTY.getMessage());
	}
}
