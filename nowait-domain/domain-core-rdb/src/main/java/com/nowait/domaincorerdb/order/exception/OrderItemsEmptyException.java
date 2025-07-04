package com.nowait.domaincorerdb.order.exception;

import com.nowait.common.exception.ErrorMessage;

public class OrderItemsEmptyException extends RuntimeException {
	public OrderItemsEmptyException() {
		super(ErrorMessage.ORDER_ITEMS_EMPTY.getMessage());
	}
}
