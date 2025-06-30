package com.nowait.applicationuser.reservation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nowait.applicationuser.reservation.dto.ReservationCreateRequestDto;
import com.nowait.applicationuser.reservation.dto.ReservationCreateResponseDto;
import com.nowait.applicationuser.reservation.service.ReservationService;
import com.nowait.common.api.ApiUtils;
import com.nowait.externaloauth.dto.CustomOAuth2User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Reservation API", description = "예약 API")
@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {

	private final ReservationService reservationService;

	@PostMapping("/create/{storeId}")
	@Operation(summary = "예약 생성", description = "특정 주점에 대한 예약하기 생성")
	@ApiResponse(responseCode = "201", description = "예약 생성")
	public ResponseEntity<?> create(
		@PathVariable Long storeId,
		@AuthenticationPrincipal CustomOAuth2User customOAuth2User,
		@RequestBody ReservationCreateRequestDto requestDto) {
		ReservationCreateResponseDto response = reservationService.create(storeId, customOAuth2User, requestDto);
		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(
				ApiUtils.success(
					response
				)
			);
	}
}
