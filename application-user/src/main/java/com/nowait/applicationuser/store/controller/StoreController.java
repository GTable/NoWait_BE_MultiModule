package com.nowait.applicationuser.store.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nowait.applicationuser.store.service.StoreService;
import com.nowait.common.api.ApiUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Store API", description = "주점(Store) API")
@RestController
@RequestMapping("v1/stores")
@RequiredArgsConstructor
@Slf4j
public class StoreController {

	private final StoreService storeService;


	@GetMapping("/all-stores")
	@Operation(summary = "모든 주점 조회", description = "모든 주점을 조회합니다.")
	@ApiResponse(responseCode = "200", description = "모든 주점 조회 성공")
	public ResponseEntity<?> getAllStores() {
		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				ApiUtils.success(
					storeService.getAllStores()
				)
			);
	}

	@GetMapping("/all-stores/infinite-scroll")
	@Operation(
		summary = "모든 주점 페이지네이션 조회",
		description = "모든 주점을 페이지네이션으로 조회합니다."
	)
	@ApiResponse(responseCode = "200", description = "모든 주점 페이지네이션 조회 성공")
	public ResponseEntity<?> getAllStores(Pageable pageable) {
		return ResponseEntity
			.ok()
			.body(ApiUtils.success(storeService.getAllStoresByPage(pageable)));
	}

	@GetMapping("/{storeId}")
	@Operation(summary = "주점 ID로 주점 상세 조회", description = "특정 주점을 ID로 조회합니다.")
	@ApiResponse(responseCode = "200", description = "주점 상세 조회 성공")
	public ResponseEntity<?> getStoreById(@PathVariable Long storeId) {
		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				ApiUtils.success(
					storeService.getStoreByStoreId(storeId)
				)
			);
	}

	@GetMapping("/search")
	@Operation(summary = "주점 이름으로 주점 검색", description = "주점 이름을 기준으로 주점을 검색합니다.")
	@ApiResponse(responseCode = "200", description = "주점 검색 성공")
	public ResponseEntity<?> searchStores(@RequestParam("name") String name) {
		return ResponseEntity
			.ok()
			.body(
				ApiUtils.success(
					storeService.searchStoresByName(name)
				)
			);
	}
}
