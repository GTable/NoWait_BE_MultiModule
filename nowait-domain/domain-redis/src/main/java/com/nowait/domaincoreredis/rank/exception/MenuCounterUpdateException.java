package com.nowait.domaincoreredis.rank.exception;

import com.nowait.common.exception.ErrorMessage;

public class MenuCounterUpdateException extends RuntimeException {
	public MenuCounterUpdateException() { super(ErrorMessage.MENU_COUNTER_UPDATE.getMessage()); }
}
