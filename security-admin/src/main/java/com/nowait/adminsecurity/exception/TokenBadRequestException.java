package com.nowait.adminsecurity.exception;

import com.nowait.token.exception.BusinessException;
import com.nowait.common.exception.ErrorMessage;

public class TokenBadRequestException extends BusinessException {
	public TokenBadRequestException() {
		super(ErrorMessage.DOES_NOT_MATCH_REFRESH_TOKEN);
	}

}
