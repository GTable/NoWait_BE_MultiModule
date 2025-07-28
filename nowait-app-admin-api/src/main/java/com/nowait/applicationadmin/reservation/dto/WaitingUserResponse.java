package com.nowait.applicationadmin.reservation.dto;

import java.time.LocalDateTime;

import com.nowait.domaincorerdb.reservation.entity.Reservation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;


@Getter
@AllArgsConstructor
@Builder
@Schema(description = "대기 사용자 응답 DTO")
public class WaitingUserResponse {

	@Schema(description = "예약 ID", example = "16-20240201-0002")
	private String reservationId;

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

	public static WaitingUserResponse fromEntity(Reservation reservation) {
		return WaitingUserResponse.builder()
			.reservationId(reservation.getId().toString())
			.userId(reservation.getUser().getId().toString())
			.partySize(reservation.getPartySize())
			.userName(reservation.getUser().getNickname())
			.createdAt(reservation.getRequestedAt())
			.status(reservation.getStatus().name())
			.build();
	}

	public static WaitingUserResponse fromRedis(String reservationId, String userId, Integer partySize, String userName, LocalDateTime createdAt, String status, Double score) {
		return WaitingUserResponse.builder()
			.reservationId(reservationId)
			.userId(userId)
			.partySize(partySize)
			.userName(userName)
			.createdAt(createdAt)
			.status(status)
			.score(score)
			.build();
	}
}
