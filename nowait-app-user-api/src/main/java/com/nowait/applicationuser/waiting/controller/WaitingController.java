package com.nowait.applicationuser.waiting.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nowait.applicationuser.waiting.dto.CancelWaitingRequest;
import com.nowait.applicationuser.waiting.dto.CancelWaitingResponse;
import com.nowait.applicationuser.waiting.dto.RegisterWaitingRequest;
import com.nowait.applicationuser.waiting.dto.RegisterWaitingResponse;
import com.nowait.applicationuser.waiting.service.WaitingService;
import com.nowait.common.api.ApiUtils;
import com.nowait.domainuserrdb.oauth.dto.CustomOAuth2User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Tag(name = "Waiting API", description = "예약 API -리팩토링 중-")
@RestController
@RequestMapping("/v2/users/me/waitings")
@RequiredArgsConstructor
public class WaitingController {
	private final WaitingService waitingService;

	/**
	 * 대기열 리팩토링용 API
	 */
	@PostMapping("/{publicCode}")
	@Operation(summary = "대기열 리팩토링용 API", description = "대기열 리팩토링용 API")
	public ResponseEntity<?> registerWaiting(
		@AuthenticationPrincipal CustomOAuth2User customOAuth2User,
		@PathVariable String publicCode,
		@RequestBody RegisterWaitingRequest request,
		HttpServletRequest httpServletRequest
	) {
		RegisterWaitingResponse registerWaitingResponse = waitingService.registerWaiting(
			customOAuth2User,
			publicCode,
			request,
			httpServletRequest
		);

		return ResponseEntity
			.ok()
			.body(
				ApiUtils.success(
					registerWaitingResponse
				)
			);
	}

	@DeleteMapping("/{publicCode}")
	@Operation(summary = "대기열 리팩토링용 API", description = "대기 취소")
	public ResponseEntity<?> cancelWaiting(
		@AuthenticationPrincipal CustomOAuth2User customOAuth2User,
		@PathVariable String publicCode,
		@RequestBody CancelWaitingRequest request
	) {
		CancelWaitingResponse cancelWaitingResponse = waitingService.cancelWaiting(
			customOAuth2User,
			publicCode,
			request
		);

		return ResponseEntity
			.ok()
			.body(
				ApiUtils.success(
					cancelWaitingResponse
				)
			);
	}
}
