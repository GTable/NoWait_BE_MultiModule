package com.nowait.applicationuser.menu.service;

import org.springframework.stereotype.Service;

import com.nowait.applicationuser.menu.dto.MenuReadDto;
import com.nowait.applicationuser.menu.dto.MenuReadResponse;
import com.nowait.applicationuser.menu.repository.DistributedCachedMenuDao;
import com.nowait.domaincorerdb.menu.exception.MenuNotFoundException;
import com.nowait.domaincorerdb.menu.exception.MenuParamEmptyException;
import com.nowait.domaincorerdb.store.entity.Store;
import com.nowait.domaincorerdb.store.exception.StoreNotFoundException;
import com.nowait.domaincorerdb.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuService {

	private final StoreRepository storeRepository;
	private final DistributedCachedMenuDao menuDao;

	// @Transactional 을 걸지 않는다: 주점 조회로 잡힌 스냅샷 안에서 캐시 미스 로딩이 일어나면
	// 무효화 직후에도 커밋 전 메뉴가 TTL 동안 캐싱될 수 있다
	public MenuReadResponse getAllMenusByStoreId(String publicCode) {
		if (publicCode == null) {
			throw new MenuParamEmptyException();
		}

		Store store = storeRepository.findByPublicCodeAndDeletedFalse(publicCode)
			.orElseThrow(StoreNotFoundException::new);

		return MenuReadResponse.of(store.getName(), menuDao.get(String.valueOf(store.getStoreId())));
	}

	public MenuReadDto getMenuById(String publicCode, Long menuId) {
		if (publicCode == null || menuId == null) {
			throw new MenuParamEmptyException();
		}

		Store store = storeRepository.findByPublicCodeAndDeletedFalse(publicCode)
			.orElseThrow(StoreNotFoundException::new);

		return menuDao.get(String.valueOf(store.getStoreId())).stream()
			.filter(menu -> menu.getMenuId().equals(menuId))
			.findFirst()
			.orElseThrow(MenuNotFoundException::new);
	}
}
