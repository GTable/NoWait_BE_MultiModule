package com.nowait.applicationuser.reservation.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReservationCreateResponseDto {
	private Long id;
	private Long storeId;
	private Long userId;
	private LocalDateTime requestedAt;
	private String status;
	private Integer partySize;
}
