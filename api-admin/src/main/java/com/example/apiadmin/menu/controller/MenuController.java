package com.example.apiadmin.menu.controller;

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

import com.example.apiadmin.menu.dto.MenuCreateRequest;
import com.example.apiadmin.menu.dto.MenuCreateResponse;
import com.example.apiadmin.menu.dto.MenuUpdateRequest;
import com.example.apiadmin.menu.service.MenuService;
import com.nowaiting.common.api.ApiUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/menus")
@RequiredArgsConstructor
public class MenuController {

	private final MenuService menuService;

	@PostMapping("/create")
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


	@PatchMapping("/update/{menuId}")
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
