package com.example.apiadmin.reservation.dto;

import com.nowaiting.common.enums.ReservationStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReservationStatusUpdateRequestDto {
	private ReservationStatus status;
}
