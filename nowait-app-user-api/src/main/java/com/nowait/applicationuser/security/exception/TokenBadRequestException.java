package com.nowait.applicationuser.security.exception;

import com.nowait.common.exception.ErrorMessage;
import com.nowait.domaincorerdb.token.exception.BusinessException;

public class TokenBadRequestException extends BusinessException {
	public TokenBadRequestException() {
		super(ErrorMessage.DOES_NOT_MATCH_REFRESH_TOKEN);
	}

}
