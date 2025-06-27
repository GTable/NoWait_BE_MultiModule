package com.nowait.domainorder.exception;

import com.nowaiting.common.exception.ErrorMessage;

public class OrderItemsEmptyException extends RuntimeException {
	public OrderItemsEmptyException() {
		super(ErrorMessage.ORDER_ITEMS_EMPTY.getMessage());
	}
}
