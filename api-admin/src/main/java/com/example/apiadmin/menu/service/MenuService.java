package com.example.apiadmin.menu.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.apiadmin.menu.dto.MenuCreateRequest;
import com.example.apiadmin.menu.dto.MenuCreateResponse;
import com.example.apiadmin.menu.dto.MenuImageUploadResponse;
import com.example.apiadmin.menu.dto.MenuReadDto;
import com.example.apiadmin.menu.dto.MenuReadResponse;
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
	public MenuReadResponse getMenusByStoreId(Long storeId) {
		List<Menu> menus = menuRepository.findAllByStoreId(storeId);

		List<MenuReadDto> menuReadRespons = menus.stream()
			.map(menu -> {
				List<MenuImage> images = menuImageRepository.findByMenu(menu);
				List<MenuImageUploadResponse> imageDto = images.stream()
					.map(MenuImageUploadResponse::fromEntity)
					.toList();
				return MenuReadDto.fromEntity(menu, imageDto);
			})
			.toList();

		return MenuReadResponse.of(menuReadRespons);
	}
}
