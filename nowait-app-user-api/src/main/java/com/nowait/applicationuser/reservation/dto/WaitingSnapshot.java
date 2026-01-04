package com.nowait.applicationuser.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WaitingSnapshot {
	private final Long rank;
	private final Integer partySize;
	private final String reservationId;
}
