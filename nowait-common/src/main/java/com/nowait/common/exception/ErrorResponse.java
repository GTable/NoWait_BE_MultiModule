package com.nowait.common.exception;

import java.util.HashMap;
import java.util.Map;


public class ErrorResponse {
	private final String message;
	private final String code;
	private final Map<String, String> errors;

	public ErrorResponse(String message, String code) {
		this.message = message;
		this.code = code;
		errors = new HashMap<>();
	}

	public ErrorResponse(String message, String code, Map<String, String> errors) {
		this.message = message;
		this.code = code;
		this.errors = errors;
	}

	public String getMessage() {
		return message;
	}

	public String getCode() {
		return code;
	}

	public Map<String, String> getErrors() {
		return errors;
	}
}
