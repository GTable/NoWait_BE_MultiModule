package com.nowait.domaincorerdb.menu.exception;

import com.nowait.common.exception.ErrorMessage;

public class MenuUpdateUnauthorizedException extends RuntimeException {
	public MenuUpdateUnauthorizedException() {
		super(ErrorMessage.MENU_UPDATE_UNAUTHORIZED.getMessage());
	}
}
