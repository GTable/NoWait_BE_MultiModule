package com.example.apiadmin.reservation.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ReservationStatusSummaryDto {
	private int waitingCount;
	private int confirmedCount;
	private int cancelledCount;
	private int callingCount;
	private List<ReservationGetResponseDto> reservationList;
}
