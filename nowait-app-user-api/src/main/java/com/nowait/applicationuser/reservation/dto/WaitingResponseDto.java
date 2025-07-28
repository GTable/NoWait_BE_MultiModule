package com.nowait.applicationuser.reservation.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WaitingResponseDto {
	private final String reservationNumber;
	private final int rank;
	private final boolean reserved; // true면 예약, false면 대기
	private final int partySize;
}
