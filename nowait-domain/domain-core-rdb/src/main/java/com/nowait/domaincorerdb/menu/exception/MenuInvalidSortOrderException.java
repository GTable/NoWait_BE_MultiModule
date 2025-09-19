package com.nowait.domaincorerdb.menu.exception;

import com.nowait.common.exception.ErrorMessage;

public class MenuInvalidSortOrderException extends RuntimeException {
	public MenuInvalidSortOrderException() {
		super(ErrorMessage.MENU_INVALID_SORT_ORDER.getMessage());
	}
}
