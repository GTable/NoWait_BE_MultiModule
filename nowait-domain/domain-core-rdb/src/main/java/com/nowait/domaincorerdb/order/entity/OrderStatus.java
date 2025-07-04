package com.nowait.domaincorerdb.order.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// TODO 자바 코드로 변경 후 common 모듈로 이동
@Getter
@RequiredArgsConstructor
@Schema(description = "주문 상태 Enum")
public enum OrderStatus {
	@Schema(description = "입금대기")
	WAITING_FOR_PAYMENT("입금대기"),

	@Schema(description = "조리중")
	COOKING("조리중"),

	@Schema(description = "조리완료")
	COOKED("조리완료");

	private final String description;
}

