package com.nowait.frontsecurity.exception;

import com.nowait.common.exception.ErrorMessage;
import com.nowait.token.exception.BusinessException;

public class TokenBadRequestException extends BusinessException {
	public TokenBadRequestException() {
		super(ErrorMessage.DOES_NOT_MATCH_REFRESH_TOKEN);
	}

}
