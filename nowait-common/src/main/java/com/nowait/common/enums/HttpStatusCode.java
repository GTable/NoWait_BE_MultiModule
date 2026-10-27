package com.nowait.common.enums;

public enum HttpStatusCode {
	OK(200),
	BAD_REQUEST(400),
	UNAUTHORIZED(401),
	FORBIDDEN(403),
	NOT_FOUND(404),
	INTERNAL_SERVER_ERROR(500);
	// 필요시 추가…

	private final int code;
	HttpStatusCode(int code) { this.code = code; }
	public int value() { return code; }
}
