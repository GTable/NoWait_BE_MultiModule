package com.nowait.applicationadmin.reservation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nowait.applicationadmin.reservation.dto.CallGetResponseDto;
import com.nowait.applicationadmin.reservation.dto.ReservationStatusSummaryDto;
import com.nowait.applicationadmin.reservation.dto.ReservationStatusUpdateRequestDto;
import com.nowait.applicationadmin.reservation.service.ReservationService;
import com.nowait.common.api.ApiUtils;

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

	@GetMapping("/admin/{storeId}")
	@Operation(summary = "주점별 예약리스트 조회", description = "특정 주점에 대한 예약리스트 조회")
	@ApiResponse(responseCode = "200", description = "예약리스트 조회")
	public ResponseEntity<?> getReservationListByStoreId(@PathVariable Long storeId) {
		ReservationStatusSummaryDto response = reservationService.getReservationListByStoreId(storeId);
		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				ApiUtils.success(
					response
				)
			);
	}

	@PatchMapping("/admin/updates/{reservationId}")
	@Operation(summary = "예약팀 상태 변경", description = "특정 예약에 대한 상태 변경(예약중->호출중,호출중->입장완료,취소)")
	@ApiResponse(responseCode = "200", description = "예약팀 상태 변경")
	public ResponseEntity<?> updateReservationStatus(@PathVariable Long reservationId,@RequestBody
	ReservationStatusUpdateRequestDto requestDto) {
		CallGetResponseDto response = reservationService.updateReservationStatus(reservationId,requestDto);
		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				ApiUtils.success(
					response
				)
			);
	}


}
