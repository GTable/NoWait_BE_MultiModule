package com.nowait.domaincorerdb.menu.exception;

import com.nowait.common.exception.ErrorMessage;

public class MenuCrossStoreConflictException extends RuntimeException {
	public MenuCrossStoreConflictException() {
		super(ErrorMessage.MENU_CROSS_STORE_CONFLICT.getMessage());
	}
}
