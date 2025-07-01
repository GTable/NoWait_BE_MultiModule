package com.nowait.menu.exception;

import com.nowait.common.exception.ErrorMessage;

public class MenuNotFoundException extends RuntimeException {
	public MenuNotFoundException() {
		super(ErrorMessage.MENU_NOT_FOUND.getMessage());
	}
}
