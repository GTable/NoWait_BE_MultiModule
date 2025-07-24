package com.nowait.applicationadmin.reservation.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nowait.applicationadmin.reservation.dto.CallingWaitingResponseDto;
import com.nowait.applicationadmin.reservation.dto.WaitingUserResponse;
import com.nowait.applicationadmin.reservation.service.ReservationService;
import com.nowait.common.api.ApiUtils;
import com.nowait.common.enums.ReservationStatus;
import com.nowait.domaincorerdb.user.entity.MemberDetails;

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

	@GetMapping("/admin/{storeId}/waiting/users")
	@Operation(summary = "주점별 전체 대기/호출중 리스트 조회", description = "주점에 대한 대기 리스트 조회(WAITING,CALLING)")
	@ApiResponse(responseCode = "200", description = "주점별 전체 대기 리스트 조회")
	public ResponseEntity<List<WaitingUserResponse>> getWaitingUsersWithScore(@PathVariable Long storeId) {
		List<WaitingUserResponse> response = reservationService.getAllWaitingUserDetails(storeId);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/admin/{storeId}/completed")
	@Operation(summary = "주점별 전체 취소/완료 리스트 조회", description = "주점에 대한 완료/취소 리스트 조회(CANCELED,CONFIRMED)")
	@ApiResponse(responseCode = "200", description = "주점별 전체 완료/취소 리스트 조회")
	public ResponseEntity<?> getCompletedReservationList(
		@PathVariable Long storeId,
		@AuthenticationPrincipal MemberDetails memberDetails
	) {
		List<WaitingUserResponse> response = reservationService.getCompletedWaitingUserDetails(storeId);
		return ResponseEntity.ok(response);
	}

	@PatchMapping("/admin/{storeId}/call/{userId}")
	@Operation(summary = "예약팀 호출", description = "특정 예약에 대한 호출 진행(호출하는 순간 10분 타임어택)")
	@ApiResponse(responseCode = "200", description = "예약팀 상태 변경 :  WAITING -> CALLING")
	public ResponseEntity<?> callWaiting(@PathVariable Long storeId,
		@PathVariable String userId,
		@AuthenticationPrincipal MemberDetails memberDetails) {
		CallingWaitingResponseDto response = reservationService.callWaiting(storeId, userId, memberDetails);
		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				ApiUtils.success(
					response
				)
	);
	}
	@PostMapping("/admin/update/{storeId}/{userId}/{status}")
	@Operation(summary = "예약팀 상태 업데이트 처리", description = "특정 예약에 대한 입장 완료 처리")
	@ApiResponse(responseCode = "200", description = "예약팀 상태 변경 :  CALLING -> CONFIRMED")
	public ResponseEntity<?> updateEntry(
		@PathVariable Long storeId,
		@PathVariable String userId,
		@PathVariable ReservationStatus status,
		@AuthenticationPrincipal MemberDetails memberDetails
	) {
		String response = reservationService.processEntryStatus(storeId, userId, memberDetails,status);
		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				ApiUtils.success(
					response
				));
	}


}
