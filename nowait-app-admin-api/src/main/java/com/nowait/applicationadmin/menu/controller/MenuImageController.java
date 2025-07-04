package com.nowait.applicationadmin.menu.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nowait.applicationadmin.menu.dto.MenuImageUploadResponse;
import com.nowait.applicationadmin.menu.service.MenuImageService;
import com.nowait.common.api.ApiUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Menu Image API", description = "메뉴 이미지 API")
@RestController
@RequestMapping("/admin/menus")
@RequiredArgsConstructor
@Slf4j
public class MenuImageController {

	private final MenuImageService menuImageService;

	@PostMapping("/images/{menuId}")
	@Operation(
		summary = "메뉴 이미지 업로드",
		description = "특정 메뉴에 대한 이미지를 업로드합니다. 파일 크기는 최대 10MB로 제한됩니다."
	)
	@ApiResponse(responseCode = "201", description = "메뉴 이미지 업로드 성공")
	public ResponseEntity<?> uploadMenuImage(
		@PathVariable Long menuId,
		@RequestParam("file") MultipartFile file
	) {
		if (file.isEmpty()) {
			throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다.");
		}
		if (file.getSize() > 10 * 1024 * 1024) { // 10MB 제한
			throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다.");
		}

		MenuImageUploadResponse response = menuImageService.save(menuId, file);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiUtils.success(response));
	}

	@DeleteMapping("/images/{menuImageId}")
	@Operation(
		summary = "메뉴 이미지 삭제",
		description = "특정 메뉴 이미지 ID에 해당하는 이미지를 삭제합니다."
	)
	@ApiResponse(responseCode = "200", description = "메뉴 이미지 삭제 성공")
	public ResponseEntity<?> deleteMenuImage(@PathVariable Long id) {
		menuImageService.delete(id);
		return ResponseEntity
			.status(
				HttpStatus.OK
			)
			.body(
				ApiUtils
					.success(
						"Menu image deleted successfully."
					)
			);
	}

}
