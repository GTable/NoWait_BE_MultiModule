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
	NOT_FOUND_USER("저장된 사용자 정보가 없습니다.", "user001"),

	//order
	ORDER_PARAMETER_EMPTY("주문 생성 시 파라미터 정보가 없습니다.", "order001"),
	ORDER_ITEMS_EMPTY("주문 항목이 없습니다.", "order002"),
	DUPLICATE_ORDER("동일한 주문이 접수되었습니다.", "order003"),
	DEPOSITOR_NAME_TOO_LONG("주문자명은 10자 이내 글자열입니다.", "order004"),
	ORDER_VIEW_UNAUTHORIZED("주문 보기 권한이 없습니다.(슈퍼계정 or 주점 관리자만 가능)", "order005"),
	ORDER_NOT_FOUND("해당 주문을 찾을 수 없습니다.", "order006"),
	ORDER_UPDATE_UNAUTHORIZED("주문 수정 권한이 없습니다.(슈퍼계정 or 주점 관리자만 가능)", "order007"),
	ORDER_ALREADY_CANCELLED("이미 취소된 주문입니다.", "order008"),
	INVALID_ORDER_STATUS_TRANSITION("유효하지 않은 주문 상태 변경입니다. (현재: %s, 요청: %s)", "order009"),

	//reservation
	NOTFOUND_RESERVATION("저장된 예약 정보가 없습니다.", "reservation001"),
	RESERVATION_VIEW_UNAUTHORIZED("예약 보기 권한이 없습니다.(슈퍼계정 or 주점 관리자만 가능)", "reservation002"),
	RESERVATION_UPDATE_UNAUTHORIZED("예약 수정 권한이 없습니다.(슈퍼계정 or 주점 관리자만 가능)", "reservation003"),
	DUPLICATE_RESERVATION("이미 대기 중인 예약이 존재합니다.", "reservation004"),
	USER_WAITING_LIMIT_EXCEEDED("유저당 웨이팅 가능 개수(3개)를 초과했습니다.", "reservation005"),
	RESERVATION_NUMBER_ISSUE_FAIL("예약 번호 발급에 실패했습니다.", "reservation006"),
	RESERVATION_ADD_UNAUTHORIZED("MANAGER는 예약 대기를 할 수 없습니다.", "reservation007"),

	// bookmark
	DUPLICATE_BOOKMARK("이미 북마크한 주점입니다.", "bookmark001"),
	NOT_OWN_BOOKMARK("해당 주점은 다른 사용자가 북마크한 주점입니다.", "bookmark002"),
	NOT_FOUND_BOOKMARK("북마크를 찾을 수 없습니다", "bookmark003"),
	ALREADY_DELETED_BOOKMARK("이미 삭제된 북마크입니다.", "bookmark004"),

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
	STORE_WAITING_DISABLED("해당 주점은 대기 비활성화된 주점입니다.", "store006"),

	// storePayment
	STORE_PAYMENT_PARAMETER_EMPTY("주점 결제 생성 시 파라미터 정보가 없습니다.", "storePayment001"),
	STORE_PAYMENT_NOT_FOUND("해당 주점 결제 정보를 찾을 수 없습니다.", "storePayment002"),
	STORE_PAYMENT_VIEW_UNAUTHORIZED("주점 결제 정보 보기 권한이 없습니다.(슈퍼계정 or 주점 관리자만 가능)", "storePayment003"),
	STORE_PAYMENT_CREATION_UNAUTHORIZED("주점 결제 정보 생성 권한이 없습니다.(슈퍼계정 or 주점 관리자만 가능)", "storePayment004"),
	STORE_PAYMENT_UPDATE_UNAUTHORIZED("주점 결제 정보 수정 권한이 없습니다.(슈퍼계정 or 주점 관리자만 가능)", "storePayment005"),
	STORE_PAYMENT_DELETE_UNAUTHORIZED("주점 결제 정보 삭제 권한이 없습니다.(슈퍼계정 or 주점 관리자만 가능)", "storePayment006"),
	STORE_PAYMENT_ALREADY_EXISTS("이미 존재하는 주점 결제 정보입니다.", "storePayment007"),

	// Statistics
	STATISTIC_VIEW_UNAUTHORIZED("통계 보기 권한이 없습니다.(슈퍼계정 or 주점 관리자만 가능)", "statistics001"),
	MENU_COUNTER_UPDATE("메뉴 카운터 업데이트 실패", "statistics002"),

	// image
	IMAGE_FILE_EMPTY("업로드 된 이미지 파일이 없습니다.", "image001"),
	IMAGE_FILE_NOT_FOUND("DB에 해당 이미지 메타데이터가 존재하지 않습니다.", "image002"),

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

	public String format(Object... args) {
		return String.format(message, args);
	}
}
