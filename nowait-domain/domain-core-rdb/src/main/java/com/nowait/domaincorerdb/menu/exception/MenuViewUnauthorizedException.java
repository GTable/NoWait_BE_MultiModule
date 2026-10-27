package com.nowait.domaincorerdb.menu.exception;

import com.nowait.common.exception.ErrorMessage;

public class MenuViewUnauthorizedException extends RuntimeException {
	public MenuViewUnauthorizedException() {
		super(ErrorMessage.MENU_VIEW_UNAUTHORIZED.getMessage());
	}
}
