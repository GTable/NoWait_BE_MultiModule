package com.nowait.applicationadmin.reservation.dto;

import java.time.LocalDateTime;

import com.nowait.domaincorerdb.reservation.entity.Reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class WaitingUserResponse {
	private String id;          // userId
	private Integer partySize;
	private String userName;
	private LocalDateTime createdAt;
	private String status;
	private Double score;       // (필요시, 대기열 정렬 등)

	public static WaitingUserResponse fromEntity(Reservation reservation) {
		return WaitingUserResponse.builder()
			.id(reservation.getId().toString())
			.partySize(reservation.getPartySize())
			.userName(reservation.getUser().getNickname())
			.createdAt(reservation.getRequestedAt())
			.status(reservation.getStatus().name())
			.build();
	}
}
