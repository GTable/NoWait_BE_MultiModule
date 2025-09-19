package com.nowait.applicationadmin.menu.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationadmin.menu.dto.MenuCreateRequest;
import com.nowait.applicationadmin.menu.dto.MenuCreateResponse;
import com.nowait.applicationadmin.menu.dto.MenuImageUploadResponse;
import com.nowait.applicationadmin.menu.dto.MenuReadDto;
import com.nowait.applicationadmin.menu.dto.MenuReadResponse;
import com.nowait.applicationadmin.menu.dto.MenuSortUpdateRequest;
import com.nowait.applicationadmin.menu.dto.MenuUpdateRequest;
import com.nowait.common.enums.Role;
import com.nowait.domaincorerdb.menu.entity.Menu;
import com.nowait.domaincorerdb.menu.entity.MenuImage;
import com.nowait.domaincorerdb.menu.exception.MenuCreationUnauthorizedException;
import com.nowait.domaincorerdb.menu.exception.MenuCrossStoreConflictException;
import com.nowait.domaincorerdb.menu.exception.MenuDeleteUnauthorizedException;
import com.nowait.domaincorerdb.menu.exception.MenuDuplicateIdException;
import com.nowait.domaincorerdb.menu.exception.MenuInvalidSortOrderException;
import com.nowait.domaincorerdb.menu.exception.MenuNotFoundException;
import com.nowait.domaincorerdb.menu.exception.MenuParamEmptyException;
import com.nowait.domaincorerdb.menu.exception.MenuUpdateUnauthorizedException;
import com.nowait.domaincorerdb.menu.exception.MenuViewUnauthorizedException;
import com.nowait.domaincorerdb.menu.repository.MenuImageRepository;
import com.nowait.domaincorerdb.menu.repository.MenuRepository;
import com.nowait.domaincorerdb.order.exception.OrderUpdateUnauthorizedException;
import com.nowait.domaincorerdb.order.exception.OrderViewUnauthorizedException;
import com.nowait.domaincorerdb.user.entity.MemberDetails;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domaincorerdb.user.exception.UserNotFoundException;
import com.nowait.domaincorerdb.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuService {

	private final MenuRepository menuRepository;
	private final MenuImageRepository menuImageRepository;
	private final UserRepository userRepository;

	@Transactional
	public MenuCreateResponse createMenu(MenuCreateRequest request, MemberDetails memberDetails) {
		// 사용자 정보 가져오기
		User user = getUser(memberDetails);
		// 사용자 역할이 SUPER_ADMIN이거나, storeId가 일치하는지 확인
		validateViewAuthorization(user, request.getStoreId());

		// 메뉴 생성 로직
		Menu toSave = request.toEntity();
		Menu saved = menuRepository.save(toSave);

		return MenuCreateResponse.fromEntity(saved);
	}

	@Transactional(readOnly = true)
	public MenuReadResponse getAllMenusByStoreId(Long storeId, MemberDetails memberDetails) {
		// 사용자 정보 가져오기
		User user = getUser(memberDetails);
		// 사용자 역할이 SUPER_ADMIN이거나, storeId가 일치하는지 확인
		validateViewAuthorization(user, storeId);

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

		return MenuReadResponse.of(menuReadResponse);
	}

	@Transactional(readOnly = true)
	public MenuReadDto getMenuById(Long storeId, Long menuId, MemberDetails memberDetails) {
		if (storeId == null || menuId == null) {
			throw new MenuParamEmptyException();
		}
		// 사용자 정보 가져오기
		User user = getUser(memberDetails);
		Menu menu = getMenu(menuId);
		// 사용자 역할이 SUPER_ADMIN이거나, storeId가 일치하는지 확인
		validateViewAuthorization(user, menu.getStoreId());

		List<MenuImage> images = menuImageRepository.findByMenu(menu);
		List<MenuImageUploadResponse> imageDto = images.stream()
			.map(MenuImageUploadResponse::fromEntity)
			.toList();

		return MenuReadDto.fromEntity(menu, imageDto);

	}

	@Transactional
	public MenuReadDto updateMenu(Long menuId, MenuUpdateRequest request, MemberDetails memberDetails) {
		User user = getUser(memberDetails);
		Menu menu = getMenu(menuId);

		validateUpdateAuthorization(user, menu.getStoreId());

		menu.updateInfo(
			request.getAdminDisplayName(),
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
	public String updateMenuSortOrder(List<MenuSortUpdateRequest> requests, MemberDetails memberDetails) {
		User user = userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);

		if (!Role.SUPER_ADMIN.equals(user.getRole())) {
			throw new MenuUpdateUnauthorizedException();
		}

		if (requests == null || requests.isEmpty()) {
			throw new MenuParamEmptyException();
		}

		if (requests.stream().map(MenuSortUpdateRequest::getMenuId).distinct().count() != requests.size()) {
			throw new MenuDuplicateIdException();
		}

		if (requests.stream().anyMatch(r -> r.getSortOrder() == null || r.getSortOrder() < 0)) {
			throw new MenuInvalidSortOrderException();
		}

		List<Long> ids = requests.stream().map(MenuSortUpdateRequest::getMenuId).toList();
		List<Menu> menus = menuRepository.findAllById(ids);
		if (menus.size() != ids.size()) {
			throw new MenuNotFoundException();
		}

		Long storeId = menus.get(0).getStoreId();
		if (!menus.stream().allMatch(m -> storeId.equals(m.getStoreId()))) {
			throw new MenuCrossStoreConflictException();
		}

		Map<Long, Long> idToSort = requests.stream()
			.collect(java.util.stream.Collectors.toMap(MenuSortUpdateRequest::getMenuId,
				MenuSortUpdateRequest::getSortOrder));

		menus.forEach(m -> m.updateSortOrder(idToSort.get(m.getId())));

		menuRepository.saveAll(menus);

		return "메뉴 순서가 성공적으로 업데이트되었습니다.";
	}

	@Transactional
	public String deleteMenu(Long menuId, MemberDetails memberDetails) {
		User user = getUser(memberDetails);
		Menu menu = getMenu(menuId);

		validateDeleteAuthorization(user, menu.getStoreId());
		menu.markAsDeleted();
		menuRepository.save(menu);

		return "Menu with ID " + menuId + " 삭제되었습니다.";
	}

	@Transactional
	public Boolean toggleSoldOut(Long menuId) {
		Menu menu = menuRepository.findById(menuId)
			.orElseThrow(MenuNotFoundException::new);

		menu.toggleSoldOut();
		menuRepository.save(menu);
		return menu.getIsSoldOut();
	}


	private void validateViewAuthorization(User user, Long storeId) {
		if (!(Role.SUPER_ADMIN.equals(user.getRole())
			  || (Role.MANAGER.equals(user.getRole()) && storeId.equals(user.getStoreId())))) {
			throw new MenuViewUnauthorizedException();
		}
	}

	private void validateUpdateAuthorization(User user, Long storeId) {
		if (!(Role.SUPER_ADMIN.equals(user.getRole())
			  || (Role.MANAGER.equals(user.getRole()) && storeId.equals(user.getStoreId())))) {
			throw new MenuUpdateUnauthorizedException();
		}
	}

	private void validateDeleteAuthorization(User user, Long storeId) {
		if (!(Role.SUPER_ADMIN.equals(user.getRole())
			  || (Role.MANAGER.equals(user.getRole()) && storeId.equals(user.getStoreId())))) {
			throw new MenuDeleteUnauthorizedException();
		}
	}

	private User getUser(MemberDetails memberDetails) {
		return userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
	}

	private Menu getMenu(Long menuId) {
		return menuRepository.findByIdAndDeletedFalse(menuId)
			.orElseThrow(MenuNotFoundException::new);
	}
}
