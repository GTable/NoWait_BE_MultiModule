package com.nowait.applicationuser.waiting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WaitingIdempotencyValue {
	private String state;
	private RegisterWaitingResponse response;
}
