package com.nowait.store.exception;

import com.nowait.common.exception.ErrorMessage;

public class StoreImageEmptyException extends RuntimeException {
	public StoreImageEmptyException() {
		super(ErrorMessage.IMAGE_FILE_EMPTY.getMessage());
	}
}
