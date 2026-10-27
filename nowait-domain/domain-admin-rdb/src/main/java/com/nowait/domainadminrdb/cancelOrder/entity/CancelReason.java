package com.nowait.domainadminrdb.cancelOrder.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "주문 상태 Enum")
public enum CancelReason {

	@Schema(description = "단순 취소")
	SIMPLE_CANCEL("단순 취소"),
	@Schema(description = "메뉴 품절")
	SOLD_OUT("메뉴 품절"),
	@Schema(description = "기타")
	ETC("기타");

	private final String description;
}

