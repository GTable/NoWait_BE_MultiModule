package com.nowait.menu.exception;

import com.nowait.common.exception.ErrorMessage;

public class MenuImageNotFoundException extends RuntimeException {
	public MenuImageNotFoundException() {
		super(ErrorMessage.IMAGE_FILE_NOT_FOUND.getMessage());
	}
}
