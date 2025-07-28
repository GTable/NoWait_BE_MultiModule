package com.nowait.applicationadmin.reservation.dto;

import java.time.LocalDateTime;

import com.nowait.common.enums.ReservationStatus;
import com.nowait.domaincorerdb.reservation.entity.Reservation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

// DTO: 호출 시각(calledAt) 또는 완료 메시지(message)를 선택적으로 담습니다.
@Getter
@Builder
public class EntryStatusResponseDto {
	@Schema(description = "예약 ID", example = "1201")
	private String reservationId; // reservationId

	@Schema(description = "예약 번호", example = "23-240504-0001")
	private String reservationNumber;

	@Schema(description = "유저 ID", example = "16")
	private String userId;

	@Schema(description = "파티 인원", example = "3")
	private Integer partySize;

	@Schema(description = "사용자 이름(닉네임)", example = "혜민이")
	private String userName;

	@Schema(description = "대기 등록 시각", example = "2025-07-22T16:00:00")
	private LocalDateTime createdAt;

	@Schema(description = "호출 시각", example = "2025-07-22T16:00:00")
	private LocalDateTime calledAt; // 호출 시각

	@Schema(description = "입장 완료 처리 시각", example = "2025-07-22T16:00:00")
	private LocalDateTime confirmedAt; // 완료 시각

	@Schema(description = "웨이팅 취소 시각", example = "2025-07-22T16:00:00")
	private LocalDateTime cancelledAt; // 취소 시각

	@Schema(description = "대기 상태", example = "CALLING")
	private String status;

	@Schema(description = "대기 순번/점수", example = "2.0")
	private Double score;

	@Schema(description = "호출 메시지", example = "호출 메시지")
	private String message;

	public static EntryStatusResponseDto fromEntity(Reservation r) {
		return EntryStatusResponseDto.builder()
			.reservationId(r.getId().toString())
			.reservationNumber(r.getReservationNumber())
			.userId(r.getUser().getId().toString())
			.partySize(r.getPartySize())
			.userName(r.getUser().getNickname())
			.createdAt(r.getRequestedAt())
			.status(r.getStatus().name())
			.calledAt(r.getCalledAt())
			.confirmedAt(r.getConfirmedAt())
			.cancelledAt(r.getCancelledAt())
			.message(switch (r.getStatus()) {
				case CALLING   -> r.getUser().getNickname() + "님을 호출하였습니다.";
				case CONFIRMED -> r.getUser().getNickname() + "님의 입장이 완료되었습니다.";
				case CANCELLED -> r.getUser().getNickname() + "님의 예약이 취소되었습니다.";
				default        -> "";
			})
			.build();
	}

	private String buildMessage(ReservationStatus status, String nickname) {
		return switch (status) {
			case CALLING -> nickname + "님을 호출하였습니다.";
			case CONFIRMED -> nickname + "님의 입장이 완료되었습니다.";
			case CANCELLED -> nickname + "님의 예약이 취소되었습니다.";
			default -> "";
		};
	}
}

