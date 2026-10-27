package com.nowait.applicationuser.waiting.dto;

import java.time.LocalDateTime;

import com.nowait.common.enums.ReservationStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CancelWaitingResponse {
	private String waitingNumber;
	private Long storeId;
	private ReservationStatus reservationStatus;
	private LocalDateTime canceledAt;
	private String message;
}
