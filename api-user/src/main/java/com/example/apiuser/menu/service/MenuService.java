package com.example.apiuser.menu.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.apiuser.menu.dto.MenuImageUploadResponse;
import com.example.apiuser.menu.dto.MenuReadDto;
import com.example.apiuser.menu.dto.MenuReadResponse;
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


	@Transactional(readOnly = true)
	public MenuReadResponse getMenusByStoreId(Long storeId) {
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
}
