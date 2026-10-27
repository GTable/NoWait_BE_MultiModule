package com.nowait.applicationuser.waiting.event;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AddWaitingRegisterEvent {
	@NotNull
	private final Long storeId;
	@NotNull
	private final Long userId;
	@NotNull
	private final LocalDateTime timestamp;
}
