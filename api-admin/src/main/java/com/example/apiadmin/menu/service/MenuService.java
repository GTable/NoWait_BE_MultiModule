package com.example.apiadmin.menu.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.apiadmin.menu.dto.MenuCreateRequest;
import com.example.apiadmin.menu.dto.MenuCreateResponse;
import com.example.apiadmin.menu.dto.MenuImageUploadResponse;
import com.example.apiadmin.menu.dto.MenuReadDto;
import com.example.apiadmin.menu.dto.MenuReadResponse;
import com.example.apiadmin.menu.dto.MenuUpdateRequest;
import com.example.menu.entity.Menu;
import com.example.menu.entity.MenuImage;
import com.example.menu.repository.MenuImageRepository;
import com.example.menu.repository.MenuRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuService {

	private final MenuRepository menuRepository;
	private final MenuImageRepository menuImageRepository;

	@Transactional
	public MenuCreateResponse createMenu(MenuCreateRequest request) {
		Menu toSave = request.toEntity();

		Menu saved = menuRepository.save(toSave);

		return MenuCreateResponse.fromEntity(saved);
	}

	@Transactional(readOnly = true)
	public MenuReadResponse getAllMenusByStoreId(Long storeId) {
		List<Menu> menus = menuRepository.findAllByStoreIdAndDeletedFalse(storeId);

		List<MenuReadDto> menuReadResponse = menus.stream()
			.map(menu -> {
				List<MenuImage> images = menuImageRepository.findByMenu(menu);
				List<MenuImageUploadResponse> imageDto = images.stream()
					.map(MenuImageUploadResponse::fromEntity)
					.toList();
				return MenuReadDto.fromEntity(menu, imageDto);
			})
			.toList();

		return MenuReadResponse.of(menuReadResponse);
	}

	@Transactional(readOnly = true)
	public MenuReadDto getMenuById(Long storeId, Long menuId) {
		Menu menu = menuRepository.findByStoreIdAndIdAndDeletedFalse(storeId, menuId)
			.orElseThrow(() -> new IllegalArgumentException("Menu not found with id: " + menuId));

		List<MenuImage> images = menuImageRepository.findByMenu(menu);
		List<MenuImageUploadResponse> imageDto = images.stream()
			.map(MenuImageUploadResponse::fromEntity)
			.toList();

		return MenuReadDto.fromEntity(menu, imageDto);
	}


	@Transactional
	public MenuReadDto updateMenu(Long menuId, MenuUpdateRequest request) {
		Menu menu = menuRepository.findById(menuId)
			.orElseThrow(() -> new IllegalArgumentException("Menu not found with id: " + menuId));

		menu.updateInfo(
			request.getName(),
			request.getDescription(),
			request.getPrice()
		);

		Menu saved = menuRepository.save(menu);

		List<MenuImage> images = menuImageRepository.findByMenu(saved);
		List<MenuImageUploadResponse> imageDto = images.stream()
			.map(MenuImageUploadResponse::fromEntity)
			.toList();

		return MenuReadDto.fromEntity(saved, imageDto);
	}

	@Transactional
	public String deleteMenu(Long menuId) {
		Menu menu = menuRepository.findById(menuId)
			.orElseThrow(() -> new IllegalArgumentException("Menu not found with id: " + menuId));

		menu.markAsDeleted();
		menuRepository.save(menu);

		return "Menu ID " + menuId + " 삭제되었습니다.";
	}

	@Transactional
	public Boolean toggleSoldOut(Long menuId) {
		Menu menu = menuRepository.findById(menuId)
			.orElseThrow(() -> new IllegalArgumentException("해당 메뉴가 존재하지 않습니다."));

		menu.toggleSoldOut();
		return menu.getIsSoldOut();
	}

}
