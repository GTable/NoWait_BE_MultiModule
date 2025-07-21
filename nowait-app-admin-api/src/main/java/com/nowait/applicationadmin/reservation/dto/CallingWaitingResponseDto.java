package com.nowait.applicationadmin.reservation.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "관리자 호출(호출 상태 변경) 응답 DTO")
public class CallingWaitingResponseDto {
	@Schema(description = "매장 ID", example = "7")
	private Long storeId;

	@Schema(description = "유저 ID", example = "123")
	private String userId;

	@Schema(description = "대기 상태", example = "CALLING")
	private String status;

	@Schema(description = "호출 시각", example = "2025-07-21T17:01:00")
	private LocalDateTime calledAt;

	@Schema(description = "대기 순번", example = "3")
	private Integer rank;

	@Schema(description = "파티 인원", example = "4")
	private Integer partySize;


}
