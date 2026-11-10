package com.nowait.applicationuser.idempotency;

import com.nowait.common.exception.ErrorMessage;
import com.nowait.domaincorerdb.token.exception.BusinessException;

// BusinessException 핸들러가 400 으로 응답한다
public class InvalidIdempotencyKeyException extends BusinessException {
	public InvalidIdempotencyKeyException() { super(ErrorMessage.INVALID_IDEMPOTENCY_KEY); }
}
