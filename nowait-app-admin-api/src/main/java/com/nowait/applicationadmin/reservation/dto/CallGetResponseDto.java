package com.nowait.applicationadmin.reservation.dto;

import java.time.LocalDateTime;

import com.nowait.domaincorerdb.reservation.entity.Reservation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "호출(대기) 사용자 응답 DTO")
public class CallGetResponseDto {
	@Schema(description = "예약 ID", example = "123")
	private Long id;

	@Schema(description = "가게 ID", example = "99")
	private Long storeId;

	@Schema(description = "사용자 닉네임", example = "혜민이")
	private String userName;

	@Schema(description = "예약 요청 시각", example = "2025-07-22T14:45:09.492253")
	private LocalDateTime requestedAt;

	@Schema(description = "예약 상태", example = "CALLING")
	private String status;

	@Schema(description = "파티 인원 수", example = "4")
	private Integer partySize;

	public static CallGetResponseDto fromEntity(Reservation reservation) {
		return CallGetResponseDto.builder()
			.id(reservation.getId())
			.storeId(reservation.getStore().getStoreId())
			.userName(reservation.getUser().getNickname())
			.requestedAt(reservation.getRequestedAt())
			.status(reservation.getStatus().name())
			.partySize(reservation.getPartySize())
			.build();
	}
}
