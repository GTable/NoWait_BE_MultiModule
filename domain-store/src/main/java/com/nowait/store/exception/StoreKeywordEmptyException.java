package com.nowait.store.exception;

import com.nowait.common.exception.ErrorMessage;

public class StoreKeywordEmptyException extends RuntimeException {
	public StoreKeywordEmptyException() {
		super(ErrorMessage.STORE_PARAMETER_EMPTY.getMessage());
	}
}
