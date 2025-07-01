package com.nowait.applicationadmin.menu.controller;

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

import com.nowait.applicationadmin.menu.dto.MenuCreateRequest;
import com.nowait.applicationadmin.menu.dto.MenuCreateResponse;
import com.nowait.applicationadmin.menu.dto.MenuUpdateRequest;
import com.nowait.applicationadmin.menu.service.MenuService;
import com.nowait.common.api.ApiUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Menu API", description = "메뉴 API")
@RestController
@RequestMapping("/admin/menus")
@RequiredArgsConstructor
@Slf4j
public class MenuController {

	private final MenuService menuService;

	@PostMapping("/create")
	@Operation(summary = "메뉴 생성", description = "새로운 메뉴를 생성합니다.")
	@ApiResponse(responseCode = "201", description = "메뉴 생성")
	public ResponseEntity<?> createMenu(@Valid @RequestBody MenuCreateRequest request) {
		MenuCreateResponse response = menuService.createMenu(request);

		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(
				ApiUtils.success(
					response
				)
			);
	}

	@GetMapping("/all-menus/stores/{storeId}")
	@Operation(summary = "가게의 모든 메뉴 조회", description = "특정 가게의 모든 메뉴를 조회")
	@ApiResponse(responseCode = "200", description = "가게의 모든 메뉴 조회")
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
	@Operation(summary = "메뉴 상세 조회", description = "특정 메뉴의 상세 정보를 조회")
	@ApiResponse(responseCode = "200", description = "메뉴 상세 조회")
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


	@PatchMapping("/update/{menuId}")
	@Operation(summary = "메뉴 수정", description = "특정 메뉴의 정보를 수정합니다.")
	@ApiResponse(responseCode = "200", description = "메뉴 수정")
	public ResponseEntity<?> updateMenu(
		@PathVariable Long menuId,
		@Valid @RequestBody MenuUpdateRequest request
	) {
		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				ApiUtils.success(
					menuService.updateMenu(menuId, request)
				)
			);
	}

	@DeleteMapping("/delete/{menuId}")
	@Operation(summary = "메뉴 삭제", description = "특정 메뉴를 삭제합니다.")
	@ApiResponse(responseCode = "200", description = "메뉴 삭제")
	public ResponseEntity<?> deleteMenu(@PathVariable Long menuId) {
		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				ApiUtils.success(
					menuService.deleteMenu(menuId)
				)
			);
	}

	@PatchMapping("/toggle-soldout/{menuId}")
	@Operation(summary = "메뉴 판매 중지/재개", description = "특정 메뉴의 판매 상태를 판매 중지 또는 재개로 토글합니다.")
	@ApiResponse(responseCode = "200", description = "메뉴 판매 상태 토글")
	public ResponseEntity<?> toggleSoldOut(@PathVariable Long menuId) {
		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				ApiUtils.success(
					menuService.toggleSoldOut(menuId)
				)
			);
	}
}
