package com.nowait.applicationadmin.reservation.dto;

import java.time.LocalDateTime;

import com.nowait.reservation.entity.Reservation;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReservationGetResponseDto {
	private Long id;
	private Long storeId;
	private String userName;
	private LocalDateTime requestedAt;
	private String status;
	private Integer partySize;

	public static ReservationGetResponseDto fromEntity(Reservation reservation) {
		return ReservationGetResponseDto.builder()
			.id(reservation.getId())
			.storeId(reservation.getStore().getStoreId())
			.userName(reservation.getUser().getNickname())
			.requestedAt(reservation.getRequestedAt())
			.status(reservation.getStatus().name())
			.partySize(reservation.getPartySize())
			.build();
	}
}
