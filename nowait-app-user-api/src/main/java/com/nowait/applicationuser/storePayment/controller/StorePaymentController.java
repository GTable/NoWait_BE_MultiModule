package com.nowait.applicationuser.storePayment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nowait.applicationuser.storePayment.service.StorePaymentService;
import com.nowait.common.api.ApiUtils;
import com.nowait.domaincorerdb.user.entity.MemberDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Store Payment API", description = "주점 결제 정보 API")
@RestController
@RequestMapping("v1/store-payments")
@RequiredArgsConstructor
@Slf4j
public class StorePaymentController {

	private final StorePaymentService storePaymentService;

	@GetMapping(("/{storeId}"))
	@Operation(summary = "주점 결제 정보 조회", description = "주점 ID로 주점 결제 정보를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "주점 결제 정보 조회 성공")
	public ResponseEntity<?> getStorePaymentByStoreId(@Valid @PathVariable Long storeId) {
		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				ApiUtils.success(
					storePaymentService.getStorePaymentByStoreId(storeId)
				)
			);
	}
}
