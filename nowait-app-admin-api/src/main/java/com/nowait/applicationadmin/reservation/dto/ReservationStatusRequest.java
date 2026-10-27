package com.nowait.applicationadmin.reservation.dto;

import com.nowait.common.enums.ReservationStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationStatusRequest {
	private ReservationStatus status;
}
