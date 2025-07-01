package com.nowait.menu.exception;

import com.nowait.common.exception.ErrorMessage;

public class MenuParamEmptyException extends RuntimeException {
	public MenuParamEmptyException() {
		super(ErrorMessage.MENU_PARAMETER_EMPTY.getMessage());
	}
}
