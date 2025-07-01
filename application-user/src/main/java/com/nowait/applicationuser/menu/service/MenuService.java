package com.nowait.applicationuser.menu.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationuser.menu.dto.MenuImageUploadResponse;
import com.nowait.applicationuser.menu.dto.MenuReadDto;
import com.nowait.applicationuser.menu.dto.MenuReadResponse;
import com.nowait.menu.entity.Menu;
import com.nowait.menu.entity.MenuImage;
import com.nowait.menu.exception.MenuNotFoundException;
import com.nowait.menu.exception.MenuParamEmptyException;
import com.nowait.menu.repository.MenuImageRepository;
import com.nowait.menu.repository.MenuRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuService {

	private final MenuRepository menuRepository;
	private final MenuImageRepository menuImageRepository;


	@Transactional(readOnly = true)
	public MenuReadResponse getAllMenusByStoreId(Long storeId) {
		if (storeId == null) {
			throw new MenuParamEmptyException();
		}
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
		if (storeId == null || menuId == null) {
			throw new MenuParamEmptyException();
		}

		Menu menu = menuRepository.findByStoreIdAndIdAndDeletedFalse(storeId, menuId)
			.orElseThrow(MenuNotFoundException::new);

		List<MenuImage> images = menuImageRepository.findByMenu(menu);
		List<MenuImageUploadResponse> imageDto = images.stream()
			.map(MenuImageUploadResponse::fromEntity)
			.toList();

		return MenuReadDto.fromEntity(menu, imageDto);
	}
}
