package com.nowait.applicationadmin.store.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nowait.applicationadmin.store.dto.StoreCreateRequest;
import com.nowait.applicationadmin.store.dto.StoreCreateResponse;
import com.nowait.applicationadmin.store.dto.StoreUpdateRequest;
import com.nowait.applicationadmin.store.service.StoreService;
import com.nowait.common.api.ApiUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Store API", description = "주점(Store) API")
@RestController
@RequestMapping("admin/stores")
@RequiredArgsConstructor
@Slf4j
public class StoreController {

	private final StoreService storeService;

	// 주점 생성
	@PostMapping("/create")
	@Operation(summary = "주점 생성", description = "새로운 주점을 생성합니다.")
	@ApiResponse(responseCode = "201", description = "주점 생성 성공")
	public ResponseEntity<?> createStore(@Valid @RequestBody StoreCreateRequest request) {
		StoreCreateResponse response = storeService.createStore(request);

		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(
				ApiUtils.success(
					response
				)
			);
	}

	@GetMapping("/{storeId}")
	@Operation(summary = "주점 조회", description = "주점 ID로 주점을 조회합니다.")
	@ApiResponse(responseCode = "200", description = "주점 조회 성공")
	public ResponseEntity<?> getStoreById(@PathVariable Long storeId) {
		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				ApiUtils.success(
					storeService.getStoreByStoreId(storeId)
				)
			);
	}

	@PatchMapping("/{storeId}")
	@Operation(summary = "주점 정보 수정", description = "주점 ID로 주점 정보를 수정합니다.")
	@ApiResponse(responseCode = "200", description = "주점 정보 수정 성공")
	public ResponseEntity<?> updateStore(
		@PathVariable Long storeId,
		@Valid @RequestBody StoreUpdateRequest request
	) {
		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				ApiUtils.success(
					storeService.updateStore(storeId, request)
				)
			);
	}

	@DeleteMapping("/{storeId}")
	@Operation(summary = "주점 삭제", description = "주점 ID로 주점을 삭제합니다.")
	@ApiResponse(responseCode = "200", description = "주점 삭제 성공")
	public ResponseEntity<?> deleteStore(@PathVariable Long storeId) {
		return ResponseEntity
			.ok()
			.body(
				ApiUtils.success(
					storeService.deleteStore(storeId)
				)
			);
	}

	@PatchMapping("/toggle-active/{storeId}")
	@Operation(summary = "주점 활성화/비활성화 토글", description = "주점 ID로 주점의 활성화 상태를 토글합니다.")
	@ApiResponse(responseCode = "200", description = "주점 활성화/비활성화 토글 성공")
	public ResponseEntity<?> toggleActive(@PathVariable Long storeId) {
		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				ApiUtils.success(
					storeService.toggleActive(storeId)
				)
			);
	}
}
