package com.nowait.domaincorerdb.menu.exception;

import com.nowait.common.exception.ErrorMessage;

public class MenuToggleUnauthorizedException extends RuntimeException {
	public MenuToggleUnauthorizedException() {
		super(ErrorMessage.MENU_TOGGLE_UNAUTHORIZED.getMessage());
	}
}
