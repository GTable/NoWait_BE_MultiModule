package com.nowait.applicationuser.waiting.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegisterWaitingResponse {
	private final String waitingNumber;
	private final int partySize;
}
