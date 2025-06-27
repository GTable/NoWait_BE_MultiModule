package com.nowait.adminsecurity.exception;

import com.example.domaintoken.exception.BusinessException;
import com.nowaiting.common.exception.ErrorMessage;

public class TokenBadRequestException extends BusinessException {
	public TokenBadRequestException() {
		super(ErrorMessage.DOES_NOT_MATCH_REFRESH_TOKEN);
	}

}
