package com.nowait.applicationuser.idempotency;

import com.nowait.common.exception.ErrorMessage;

public class IdempotencyKeyMismatchException extends RuntimeException {
	public IdempotencyKeyMismatchException() { super(ErrorMessage.IDEMPOTENCY_KEY_MISMATCH.getMessage()); }
}
