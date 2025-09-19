package com.nowait.domaincorerdb.menu.exception;

import com.nowait.common.exception.ErrorMessage;

public class MenuDuplicateIdException extends RuntimeException {
	public MenuDuplicateIdException() {
		super(ErrorMessage.MENU_DUPLICATE_ID.getMessage());
	}
}
