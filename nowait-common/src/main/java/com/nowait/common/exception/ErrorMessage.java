package com.nowait.common.exception;

public enum ErrorMessage {
	// global
	INVALID_INPUT_VALUE("입력값이 올바르지 않습니다.", "global001"),

	// auth
	UNAUTHORIZED("권한이 없습니다", "auth001"),

	// token
	REFRESH_TOKEN_NOT_FOUND("기존 리프레시 토큰을 찾을 수 없습니다.", "token001"),
	DOES_NOT_MATCH_REFRESH_TOKEN("기존 리프레시 토큰이 일치하지 않습니다.", "token002"),

	// user
	NOTFOUND_USER("저장된 사용자 정보가 없습니다.", "user001"),

	//order
	ORDER_PARAMETER_EMPTY("주문 생성 시 파라미터 정보가 없습니다.", "order001"),
	ORDER_ITEMS_EMPTY("주문 항목이 없습니다.", "order002"),
	DUPLICATE_ORDER("동일한 주문이 접수되었습니다.", "order003"),
	DEPOSITOR_NAME_TOO_LONG("주문자명은 10자 이내 글자열입니다.", "order004"),
	ORDER_VIEW_UNAUTHORIZED("주문 보기 권한이 없습니다.(슈퍼계정 or 주점 관리자만 가능)", "order005"),
	ORDER_NOT_FOUND("해당 주문을 찾을 수 없습니다.", "order006"),
	ORDER_UPDATE_UNAUTHORIZED("주문 수정 권한이 없습니다.(슈퍼계정 or 주점 관리자만 가능)", "order007"),

	//reservation
	NOTFOUND_RESERVATION("저장된 예약 정보가 없습니다.", "reservation001"),
	RESERVATION_VIEW_UNAUTHORIZED("예약 보기 권한이 없습니다.(슈퍼계정 or 주점 관리자만 가능)", "reservation002"),
	RESERVATION_UPDATE_UNAUTHORIZED("예약 수정 권한이 없습니다.(슈퍼계정 or 주점 관리자만 가능)", "reservation003"),
	DUPLICATE_RESERVATION("이미 대기 중인 예약이 존재합니다.", "reservation004"),

	// bookmark
	DUPLICATE_BOOKMARK("이미 북마크한 주점입니다.", "bookmark001"),
	NOT_OWN_BOOKMARK("해당 주점은 다른 사용자가 북마크한 주점입니다.", "bookmark002"),

	// menu
	MENU_PARAMETER_EMPTY("메뉴 생성 시 파라미터 정보가 없습니다.", "menu001"),
	MENU_NOT_FOUND("해당 메뉴를 찾을 수 없습니다.", "menu002"),
	MENU_CREATION_UNAUTHORIZED("메뉴 생성 권한이 없습니다.(슈퍼계정 or 주점 관리자만 가능)", "menu003"),
	MENU_VIEW_UNAUTHORIZED("메뉴 보기 권한이 없습니다.(슈퍼계정 or 주점 관리자만 가능)", "menu004"),
	MENU_UPDATE_UNAUTHORIZED("메뉴 수정 권한이 없습니다.(슈퍼계정 or 주점 관리자만 가능)", "menu005"),
	MENU_DELETE_UNAUTHORIZED("메뉴 삭제 권한이 없습니다.(슈퍼계정 or 주점 관리자만 가능)", "menu006"),

	// store
	STORE_PARAMETER_EMPTY("주점 생성 시 파라미터 정보가 없습니다.", "store001"),
	STORE_NOT_FOUND("해당 주점을 찾을 수 없습니다.", "store002"),
	STORE_VIEW_UNAUTHORIZED("주점 보기 권한이 없습니다.(슈퍼계정 or 주점 관리자만 가능)", "store003"),
	STORE_UPDATE_UNAUTHORIZED("주점 수정 권한이 없습니다.(슈퍼계정 or 주점 관리자만 가능)", "store004"),
	STORE_DELETE_UNAUTHORIZED("주점 삭제 권한이 없습니다.(슈퍼계정 or 주점 관리자만 가능)", "store005"),

	// image
	IMAGE_FILE_EMPTY("이미지 파일을 업로드 해주세요", "image001"),
	IMAGE_FILE_NOT_FOUND("이미지 파일을 업로드 해주세요", "image001"),

	// search
	SEARCH_PARAMETER_EMPTY("검색어가 비어있습니다.", "search001");

	private final String message;
	private final String code;

	ErrorMessage(String message, String code) {
		this.message = message;
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public String getCode() {
		return code;
	}
}
