package com.nowait.domaincorerdb.menu.exception;

import com.nowait.common.exception.ErrorMessage;

public class MenuDeleteUnauthorizedException extends RuntimeException {
	public MenuDeleteUnauthorizedException() {
		super(ErrorMessage.MENU_DELETE_UNAUTHORIZED.getMessage());
	}
}
