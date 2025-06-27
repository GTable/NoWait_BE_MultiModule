package com.nowaiting.common.api;

import com.nowaiting.common.enums.HttpStatusCode;

/**
 * API 에러 정보
 */
public class ApiError {
	private final String message;
	private final int status;

	/**
	 * 메시지 + 상태코드 생성자
	 */
	public ApiError(String message, int status) {
		this.message = message;
		this.status = status;
	}

	/**
	 * Throwable + HttpStatusCode 생성자
	 */
	public ApiError(Throwable throwable, HttpStatusCode status) {
		this(throwable.getMessage(), status.value());
	}

	/**
	 * 메시지 + HttpStatusCode 생성자
	 */
	public ApiError(String message, HttpStatusCode status) {
		this(message, status.value());
	}

	// Explicit getters instead of Lombok
	public String getMessage() {
		return message;
	}

	public int getStatus() {
		return status;
	}
}
