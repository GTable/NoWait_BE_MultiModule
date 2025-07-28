package com.nowait.applicationadmin.reservation.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nowait.applicationadmin.reservation.dto.EntryStatusResponseDto;
import com.nowait.applicationadmin.reservation.dto.ReservationStatusRequest;
import com.nowait.applicationadmin.reservation.dto.WaitingUserResponse;
import com.nowait.applicationadmin.reservation.service.ReservationService;
import com.nowait.common.api.ApiUtils;
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
	public ResponseEntity<?> getWaitingUsersWithScore(@PathVariable Long storeId) {
		List<WaitingUserResponse> response = reservationService.getAllWaitingUserDetails(storeId);
		return ResponseEntity
			.ok()
			.body(
				ApiUtils.success(
					response
				)
			);
	}

	@GetMapping("/admin/{storeId}/completed")
	@Operation(summary = "주점별 전체 취소/완료 리스트 조회", description = "주점에 대한 완료/취소 리스트 조회(CANCELED,CONFIRMED)")
	@ApiResponse(responseCode = "200", description = "주점별 전체 완료/취소 리스트 조회")
	public ResponseEntity<?> getCompletedReservationList(
		@PathVariable Long storeId,
		@AuthenticationPrincipal MemberDetails memberDetails
	) {
		List<WaitingUserResponse> response = reservationService.getCompletedWaitingUserDetails(storeId);
		return ResponseEntity
			.ok()
			.body(
				ApiUtils.success(
					response
				)
			);
	}

	// @PatchMapping("/admin/{storeId}/call/{userId}")
	// @Operation(summary = "예약팀 호출", description = "특정 예약에 대한 호출 진행(호출하는 순간 10분 타임어택)")
	// @ApiResponse(responseCode = "200", description = "예약팀 상태 변경 :  WAITING -> CALLING")
	// public ResponseEntity<?> callWaiting(@PathVariable Long storeId,
	// 	@PathVariable String userId,
	// 	@AuthenticationPrincipal MemberDetails memberDetails) {
	// 	CallingWaitingResponseDto response = reservationService.callWaiting(storeId, userId, memberDetails);
	// 	return ResponseEntity
	// 		.status(HttpStatus.OK)
	// 		.body(
	// 			ApiUtils.success(
	// 				response
	// 			)
	// );
	// }
	@PatchMapping("/admin/update/{storeId}/{userId}")
	@Operation(summary = "예약팀 상태 업데이트 처리", description = "특정 예약에 대한 입장 완료 처리")
	@ApiResponse(responseCode = "200", description = "예약팀 상태 변경 :  CALLING -> CONFIRMED")
	public ResponseEntity<?> updateEntry(
		@PathVariable Long storeId,
		@PathVariable String userId,
		@RequestBody ReservationStatusRequest request,
		@AuthenticationPrincipal MemberDetails memberDetails
	) {
		EntryStatusResponseDto response = reservationService.processEntryStatus(storeId, userId, memberDetails, request.getStatus());
		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				ApiUtils.success(
					response
				));
	}

}
