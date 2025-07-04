package com.nowait.domaincorerdb.menu.exception;

import com.nowait.common.exception.ErrorMessage;

public class MenuImageEmptyException extends RuntimeException {
	public MenuImageEmptyException() {
		super(ErrorMessage.IMAGE_FILE_EMPTY.getMessage());
	}
}
