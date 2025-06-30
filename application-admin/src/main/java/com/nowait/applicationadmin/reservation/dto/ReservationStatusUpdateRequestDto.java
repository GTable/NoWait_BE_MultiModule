package com.nowait.applicationadmin.reservation.dto;

import com.nowait.common.enums.ReservationStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReservationStatusUpdateRequestDto {
	private ReservationStatus status;
}
