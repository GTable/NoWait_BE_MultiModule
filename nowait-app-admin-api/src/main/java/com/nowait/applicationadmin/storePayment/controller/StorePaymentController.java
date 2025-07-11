package com.nowait.applicationadmin.storePayment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nowait.applicationadmin.storePayment.dto.StorePaymentCreateRequest;
import com.nowait.applicationadmin.storePayment.dto.StorePaymentCreateResponse;
import com.nowait.applicationadmin.storePayment.dto.StorePaymentUpdateRequest;
import com.nowait.applicationadmin.storePayment.service.StorePaymentService;
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
@RequestMapping("admin/store-payments")
@RequiredArgsConstructor
@Slf4j
public class StorePaymentController {

	private final StorePaymentService storePaymentService;

	@PostMapping("/create")
	@Operation(summary = "주점 결제 Url 등록 및 생성", description = "새로운 주점 결제 정보를 생성합니다.")
	@ApiResponse(responseCode = "201", description = "주점 결제 정보 생성 성공")
	public ResponseEntity<?> createStorePayment(@Valid @RequestBody StorePaymentCreateRequest request, @AuthenticationPrincipal MemberDetails memberDetails) {
		StorePaymentCreateResponse response = storePaymentService.createStorePayment(request, memberDetails);

		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(
				ApiUtils.success(
					response
				)
			);
	}

	@GetMapping()
	@Operation(summary = "주점 결제 정보 조회", description = "인증된 사용자의 주점 결제 정보를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "주점 결제 정보 조회 성공")
	public ResponseEntity<?> getStorePaymentByStoreId(@AuthenticationPrincipal MemberDetails memberDetails) {
		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				ApiUtils.success(
					storePaymentService.getStorePaymentByStoreId(memberDetails)
				)
			);
	}

	@PatchMapping("/update")
	@Operation(summary = "주점 결제 정보 수정", description = "주점 결제 정보를 수정합니다.")
	@ApiResponse(responseCode = "200", description = "주점 결제 정보 수정 성공")
	public ResponseEntity<?> updateStorePayment(@Valid @RequestBody StorePaymentUpdateRequest request, @AuthenticationPrincipal MemberDetails memberDetails) {
		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				ApiUtils.success(
					storePaymentService.updateStorePayment(request, memberDetails)
				)
			);
	}
}
