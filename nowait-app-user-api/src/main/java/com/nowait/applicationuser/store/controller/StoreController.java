package com.nowait.applicationuser.store.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nowait.applicationuser.store.service.StoreService;
import com.nowait.common.api.ApiUtils;
import com.nowait.domainuserrdb.oauth.dto.CustomOAuth2User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Store API", description = "주점(Store) API")
@RestController
@RequestMapping("/v1/stores")
@RequiredArgsConstructor
@Slf4j
public class StoreController {

	private final StoreService storeService;


	@GetMapping
	@Operation(summary = "모든 주점 페이지네이션 조회", description = "모든 주점을 페이지네이션으로 조회합니다.")
	@ApiResponse(responseCode = "200", description = "모든 주점 페이지네이션 조회 성공")
	public ResponseEntity<?> getAllStoresByPageAndDepartments(Pageable pageable, @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {
		return ResponseEntity
			.ok()
			.body(
				ApiUtils.success(
					storeService.getAllStoresByPageAndDeparments(pageable, customOAuth2User)
				)
			);
	}

	@GetMapping("/{publicCode}")
	@Operation(summary = "주점 ID로 주점 상세 조회", description = "특정 주점을 ID로 조회합니다.")
	@ApiResponse(responseCode = "200", description = "주점 상세 조회 성공")
	public ResponseEntity<?> getStoreById(@PathVariable String publicCode, @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {
		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				ApiUtils.success(
					storeService.getStoreByPublicCode(publicCode, customOAuth2User)
				)
			);
	}

	@GetMapping("/search")
	@Operation(summary = "주점 이름으로 주점 검색", description = "주점 이름을 기준으로 주점을 검색합니다.")
	@ApiResponse(responseCode = "200", description = "주점 검색 성공")
	public ResponseEntity<?> searchStores(@RequestParam("keyword") String keyword) {
		return ResponseEntity
			.ok()
			.body(
				ApiUtils.success(
					storeService.searchByKeywordNative(keyword)
				)
			);
	}

	@GetMapping("/waiting-count")
	@Operation(summary = "예약 많은순/적은순 주점 리스트 조회", description = "desc(대기 많은순) , asc(대기 적은순)")
	@ApiResponse(responseCode = "200", description = "주점 대기순 정렬")
	public ResponseEntity<?> getStoreWaitingList(
		@RequestParam(defaultValue = "desc") String order) {
		boolean desc = !"asc".equalsIgnoreCase(order); // 기본: 대기 많은 순

		return ResponseEntity
			.ok()
			.body(
				ApiUtils.success(
					storeService.getStoresByWaitingCount(desc)
				)
			);
	}
}
