package com.nowait.domaincorerdb.menu.exception;

import com.nowait.common.exception.ErrorMessage;

public class MenuCreationUnauthorizedException extends RuntimeException {
	public MenuCreationUnauthorizedException() {
		super(ErrorMessage.MENU_CREATION_UNAUTHORIZED.getMessage());
	}
}
