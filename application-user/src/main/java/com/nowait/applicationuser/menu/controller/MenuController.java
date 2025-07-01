package com.nowait.applicationuser.menu.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nowait.applicationuser.menu.service.MenuService;
import com.nowait.common.api.ApiUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Menu API", description = "메뉴 API")
@RestController
@RequestMapping("/v1/menus")
@RequiredArgsConstructor
@Slf4j
public class MenuController {

	private final MenuService menuService;

	@GetMapping("/all-menus/stores/{storeId}")
	@Operation(summary = "가게의 모든 메뉴 조회", description = "특정 가게의 모든 메뉴를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "모든 메뉴를 조회 성공")
	public ResponseEntity<?> getMenusByStoreId(@PathVariable Long storeId) {
		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				ApiUtils.success(
					menuService.getAllMenusByStoreId(storeId)
				)
			);
	}

	@GetMapping("/{storeId}/{menuId}")
	@Operation(
		summary = "메뉴 ID로 메뉴 조회", description = "특정 가게의 특정 메뉴를 ID로 조회합니다.")
	@ApiResponse(responseCode = "200", description = "메뉴 조회 성공")
	public ResponseEntity<?> getMenuById(
		@PathVariable Long storeId,
		@PathVariable Long menuId
	) {
		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				ApiUtils.success(
					menuService.getMenuById(storeId, menuId)
				)
			);
	}
}
