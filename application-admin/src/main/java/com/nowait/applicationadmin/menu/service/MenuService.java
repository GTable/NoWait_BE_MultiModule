package com.nowait.applicationadmin.menu.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationadmin.menu.dto.MenuCreateRequest;
import com.nowait.applicationadmin.menu.dto.MenuCreateResponse;
import com.nowait.applicationadmin.menu.dto.MenuImageUploadResponse;
import com.nowait.applicationadmin.menu.dto.MenuReadDto;
import com.nowait.applicationadmin.menu.dto.MenuReadResponse;
import com.nowait.applicationadmin.menu.dto.MenuUpdateRequest;
import com.nowait.menu.entity.Menu;
import com.nowait.menu.entity.MenuImage;
import com.nowait.menu.repository.MenuImageRepository;
import com.nowait.menu.repository.MenuRepository;

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
		Menu menu = menuRepository.findByIdAndDeletedFalse(menuId)
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
			.orElseThrow(() -> new IllegalArgumentException("Menu is already deleted with id: " + menuId));

		menu.markAsDeleted();
		menuRepository.save(menu);

		return "Menu with ID " + menuId + " 삭제되었습니다.";
	}

	@Transactional
	public Boolean toggleSoldOut(Long menuId) {
		Menu menu = menuRepository.findById(menuId)
			.orElseThrow(() -> new IllegalArgumentException("해당 메뉴가 존재하지 않습니다."));

		menu.toggleSoldOut();
		menuRepository.save(menu);
		return menu.getIsSoldOut();
	}

}
