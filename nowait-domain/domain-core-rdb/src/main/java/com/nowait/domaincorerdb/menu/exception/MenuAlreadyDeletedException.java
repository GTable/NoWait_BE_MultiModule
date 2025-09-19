package com.nowait.domaincorerdb.menu.exception;

import com.nowait.common.exception.ErrorMessage;

public class MenuAlreadyDeletedException extends RuntimeException {
	public MenuAlreadyDeletedException() {
		super(ErrorMessage.MENU_ALREADY_DELETED.getMessage());
	}
}
