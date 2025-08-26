package com.nowait.applicationuser.menu.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationuser.menu.dto.MenuImageUploadResponse;
import com.nowait.applicationuser.menu.dto.MenuReadDto;
import com.nowait.applicationuser.menu.dto.MenuReadResponse;
import com.nowait.domaincorerdb.menu.entity.Menu;
import com.nowait.domaincorerdb.menu.entity.MenuImage;
import com.nowait.domaincorerdb.menu.exception.MenuNotFoundException;
import com.nowait.domaincorerdb.menu.exception.MenuParamEmptyException;
import com.nowait.domaincorerdb.menu.repository.MenuImageRepository;
import com.nowait.domaincorerdb.menu.repository.MenuRepository;
import com.nowait.domaincorerdb.store.entity.Store;
import com.nowait.domaincorerdb.store.exception.StoreNotFoundException;
import com.nowait.domaincorerdb.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuService {

	private final MenuRepository menuRepository;
	private final MenuImageRepository menuImageRepository;
	private final StoreRepository storeRepository;


	@Transactional(readOnly = true)
	public MenuReadResponse getAllMenusByStoreId(String publicCode) {
		if (publicCode == null) {
			throw new MenuParamEmptyException();
		}

		Store store = storeRepository.findByPublicCodeAndDeletedFalse(publicCode)
			.orElseThrow(StoreNotFoundException::new);

		String storeName = store.getName();
		Long storeId = store.getStoreId();
		List<Menu> menus = menuRepository.findAllByStoreIdAndDeletedFalseOrderBySortOrder(storeId);

		List<MenuReadDto> menuReadResponse = menus.stream()
			.map(menu -> {
				List<MenuImage> images = menuImageRepository.findByMenu(menu);
				List<MenuImageUploadResponse> imageDto = images.stream()
					.map(MenuImageUploadResponse::fromEntity)
					.toList();
				return MenuReadDto.fromEntity(menu, imageDto);
			})
			.toList();

		return MenuReadResponse.of(storeName, menuReadResponse);
	}

	@Transactional(readOnly = true)
	public MenuReadDto getMenuById(String publicCode, Long menuId) {
		if (publicCode == null || menuId == null) {
			throw new MenuParamEmptyException();
		}

		Store store = storeRepository.findByPublicCodeAndDeletedFalse(publicCode)
			.orElseThrow(StoreNotFoundException::new);

		Menu menu = menuRepository.findByStoreIdAndIdAndDeletedFalse(store.getStoreId(), menuId)
			.orElseThrow(MenuNotFoundException::new);

		List<MenuImage> images = menuImageRepository.findByMenu(menu);
		List<MenuImageUploadResponse> imageDto = images.stream()
			.map(MenuImageUploadResponse::fromEntity)
			.toList();

		return MenuReadDto.fromEntity(menu, imageDto);
	}
}
