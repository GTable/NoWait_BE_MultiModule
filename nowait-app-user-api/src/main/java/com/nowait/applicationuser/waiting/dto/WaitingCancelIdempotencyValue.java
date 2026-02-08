package com.nowait.applicationuser.waiting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WaitingCancelIdempotencyValue {
	private String state;
	private CancelWaitingResponse response;
}
