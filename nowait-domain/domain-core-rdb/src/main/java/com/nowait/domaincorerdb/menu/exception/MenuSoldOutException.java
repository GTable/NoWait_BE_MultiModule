package com.nowait.domaincorerdb.menu.exception;

import com.nowait.common.exception.ErrorMessage;

public class MenuSoldOutException extends RuntimeException {
	public MenuSoldOutException() {
		super(ErrorMessage.MENU_SOLD_OUT.getMessage());
	}
}
