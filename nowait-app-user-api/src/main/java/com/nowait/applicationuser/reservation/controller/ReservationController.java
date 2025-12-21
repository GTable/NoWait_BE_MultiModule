package com.nowait.applicationuser.reservation.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nowait.applicationuser.reservation.dto.MyWaitingQueueDto;
import com.nowait.applicationuser.reservation.dto.ReservationCreateRequestDto;
import com.nowait.applicationuser.reservation.dto.ReservationCreateResponseDto;
import com.nowait.applicationuser.reservation.dto.WaitingResponseDto;
import com.nowait.applicationuser.reservation.service.ReservationService;
import com.nowait.common.api.ApiUtils;
import com.nowait.domainuserrdb.oauth.dto.CustomOAuth2User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Reservation API", description = "예약 API")
@RestController
@RequestMapping("/v1/users/me/waitings")
@RequiredArgsConstructor
public class ReservationController {

	private final ReservationService reservationService;

	// 성능 비교를 위해 남겨둔 레거시 웨이팅 등록 API
	@PostMapping("/legacy/{storeId}")
	@Operation(summary = "웨이팅 등록", description = "특정 주점에 대한 웨이팅 등록 레거시")
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

	@PostMapping("/{storeId}")
	@Operation(summary = "대기열 등록", description = "특정 주점에 대한 대기열 등록")
	@ApiResponse(responseCode = "201", description = "대기열 등록")
	public ResponseEntity<?> createQueue(
		@PathVariable Long storeId,
		@AuthenticationPrincipal CustomOAuth2User customOAuth2User,
		@RequestBody ReservationCreateRequestDto requestDto
	) {
		WaitingResponseDto response = reservationService.registerWaiting(storeId, customOAuth2User, requestDto);
		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(
				ApiUtils.success(
					response
				)
			);
	}

	@GetMapping("/{storeId}")
	@Operation(summary = "특정 주점의 본인 대기열 조회", description = "특정 주점에 대한 본인 대기열 조회")
	@ApiResponse(responseCode = "200", description = "본인 대기열 조회")
	public ResponseEntity<?> getQueue(
		@PathVariable Long storeId,
		@AuthenticationPrincipal CustomOAuth2User customOAuth2User
	) {
		WaitingResponseDto response = reservationService.myWaitingInfo(storeId, customOAuth2User);
		return ResponseEntity
			.ok()
			.body(
				ApiUtils.success(
					response
				)
			);
	}

	// TODO : 대기열 취소 시 예약 번호 사용하도록 변경 (현재는 주점 아이디로 처리 중)
	@DeleteMapping("/{storeId}")
	@Operation(summary = "내 대기열 취소", description = "특정 주점에 대한 대기열 취소")
	@ApiResponse(responseCode = "200", description = "대기열 취소")
	public ResponseEntity<?> deleteQueue(
		@PathVariable Long storeId,
		@AuthenticationPrincipal CustomOAuth2User customOAuth2User
	) {
		return ResponseEntity
			.ok()
			.body(
				ApiUtils.success(
					reservationService.cancelWaiting(storeId, customOAuth2User)
				)
			);
	}

	@GetMapping
	@Operation(summary = "내 모든 대기열 리스트 확인", description = "내가 신청한 모든 대기열 리스트 확인")
	@ApiResponse(responseCode = "200", description = "대기열 리스트 조회")
	public ResponseEntity<?> getAllMyWaitings(@AuthenticationPrincipal CustomOAuth2User customOAuth2User) {
		List<MyWaitingQueueDto> response = reservationService.getAllMyWaitings(customOAuth2User);
		return ResponseEntity
			.ok()
			.body(
				ApiUtils.success(
					response
				)
			);
	}

}
