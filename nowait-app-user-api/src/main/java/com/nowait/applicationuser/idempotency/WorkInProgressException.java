package com.nowait.applicationuser.idempotency;

import com.nowait.common.exception.ErrorMessage;

public class WorkInProgressException extends RuntimeException {
	public WorkInProgressException() { super(ErrorMessage.WORK_IN_PROGRESS.getMessage()); }
}
