package com.example.apiuser.menu.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.apiuser.menu.service.MenuService;
import com.nowaiting.common.api.ApiUtils;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/menus")
@RequiredArgsConstructor
public class MenuController {

	private final MenuService menuService;

	@GetMapping("/all-menus/stores/{storeId}")
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
