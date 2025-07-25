package com.nowait.applicationadmin.reservation.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

// DTO: 호출 시각(calledAt) 또는 완료 메시지(message)를 선택적으로 담습니다.
@Getter
@Builder
public class EntryStatusResponseDto {
	@Schema(description = "예약 ID", example = "1201")
	private String id; // reservationId

	@Schema(description = "유저 ID", example = "16")
	private String userId;

	@Schema(description = "파티 인원", example = "3")
	private Integer partySize;

	@Schema(description = "사용자 이름(닉네임)", example = "혜민이")
	private String userName;

	@Schema(description = "대기 등록 시각", example = "2025-07-22T16:00:00")
	private LocalDateTime createdAt;

	@Schema(description = "대기 상태", example = "CALLING")
	private String status;

	@Schema(description = "대기 순번/점수", example = "2.0")
	private Double score;

	@Schema(description = "호출 메시지", example = "호출 메시지")
	private String message;
}

