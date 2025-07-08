package com.nowait.domaincorerdb.store.exception;

import com.nowait.common.exception.ErrorMessage;

public class StoreWaitingDisabledException extends RuntimeException {
	public StoreWaitingDisabledException() {
		super(ErrorMessage.STORE_WAITING_DISABLED.getMessage());
	}
}
