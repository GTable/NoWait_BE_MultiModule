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

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("v1/stores")
@RequiredArgsConstructor
public class StoreController {

	private final StoreService storeService;


	@GetMapping("/all-stores")
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
	public ResponseEntity<?> getAllStores(Pageable pageable) {
		return ResponseEntity
			.ok()
			.body(ApiUtils.success(storeService.getAllStoresByPage(pageable)));
	}

	@GetMapping("/{storeId}")
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
