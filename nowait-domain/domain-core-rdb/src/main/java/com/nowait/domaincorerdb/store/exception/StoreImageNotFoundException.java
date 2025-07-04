package com.nowait.domaincorerdb.store.exception;

import com.nowait.common.exception.ErrorMessage;

public class StoreImageNotFoundException extends RuntimeException {
	public StoreImageNotFoundException() {
		super(ErrorMessage.IMAGE_FILE_NOT_FOUND.getMessage());
	}
}
