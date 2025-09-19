package com.nowait.applicationadmin.store.service;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationadmin.store.dto.StoreCreateRequest;
import com.nowait.applicationadmin.store.dto.StoreCreateResponse;
import com.nowait.applicationadmin.store.dto.StoreDetailReadResponse;
import com.nowait.applicationadmin.store.dto.StoreImageUploadResponse;
import com.nowait.applicationadmin.store.dto.StoreReadDto;
import com.nowait.applicationadmin.store.dto.StoreUpdateRequest;
import com.nowait.common.enums.Role;
import com.nowait.domaincorerdb.department.entity.Department;
import com.nowait.domaincorerdb.department.repository.DepartmentRepository;
import com.nowait.domaincorerdb.order.exception.OrderUpdateUnauthorizedException;
import com.nowait.domaincorerdb.order.exception.OrderViewUnauthorizedException;
import com.nowait.domaincorerdb.reservation.exception.ReservationUpdateUnauthorizedException;
import com.nowait.domaincorerdb.store.entity.Store;
import com.nowait.domaincorerdb.store.entity.StoreImage;
import com.nowait.domaincorerdb.store.exception.StoreDeleteUnauthorizedException;
import com.nowait.domaincorerdb.store.exception.StoreNotFoundException;
import com.nowait.domaincorerdb.store.exception.StoreParamEmptyException;
import com.nowait.domaincorerdb.store.exception.StoreUpdateUnauthorizedException;
import com.nowait.domaincorerdb.store.exception.StoreViewUnauthorizedException;
import com.nowait.domaincorerdb.store.repository.StoreImageRepository;
import com.nowait.domaincorerdb.store.repository.StoreRepository;
import com.nowait.domaincorerdb.user.entity.MemberDetails;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domaincorerdb.user.exception.UserNotFoundException;
import com.nowait.domaincorerdb.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

	private final StoreRepository storeRepository;
	private final StoreImageRepository storeImageRepository;
	private final UserRepository userRepository;
	private final DepartmentRepository departmentRepository;

	@Override
	@Transactional
	public StoreCreateResponse createStore(StoreCreateRequest request) {
		if (request == null) throw new StoreParamEmptyException();

		Store toSave = request.toEntity();

		Store saved = storeRepository.save(toSave);

		return StoreCreateResponse.fromEntity(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public StoreDetailReadResponse getStoreByStoreId(Long storeId, MemberDetails memberDetails) {
		if (storeId == null) throw new StoreParamEmptyException();
		User user = getUser(memberDetails);
		validateViewAuthorization(user, storeId);

		Store store = storeRepository.findByStoreIdAndDeletedFalse(storeId)
			.orElseThrow(StoreNotFoundException::new);

		String departmentName = departmentRepository.findById(store.getDepartmentId())
			.map(Department::getName)
			.orElse("Unknown Department");

		List<StoreImage> images = storeImageRepository.findByStore(store);
		List<StoreImageUploadResponse> imageDto = images.stream()
			.map(StoreImageUploadResponse::fromEntity)
			.toList();

		return StoreDetailReadResponse.fromEntity(store, imageDto, departmentName);
	}

	@Override
	@Transactional
	public StoreReadDto updateStore(Long storeId, StoreUpdateRequest request, MemberDetails memberDetails) {
		if (storeId == null || request == null) throw new StoreParamEmptyException();

		User user = getUser(memberDetails);
		validateUpdateAuthorization(user, storeId);

		Store store = storeRepository.findByStoreIdAndDeletedFalse(storeId)
			.orElseThrow(StoreNotFoundException::new);

		store.updateInfo(
			request.getName(),
			request.getLocation(),
			request.getDescription(),
			request.getNoticeTitle(),
			request.getNoticeContent(),
			request.getOpenTime()
		);

		Store updatedStore = storeRepository.save(store);

		List<StoreImage> images = storeImageRepository.findByStore(updatedStore);
		List<StoreImageUploadResponse> imageDto = images.stream()
			.map(StoreImageUploadResponse::fromEntity)
			.toList();

		return StoreReadDto.fromEntity(updatedStore, imageDto);
	}

	@Override
	@Transactional
	public String deleteStore(Long storeId, MemberDetails memberDetails) {

		if (storeId == null) {
			throw new StoreParamEmptyException();
		}

		User user = getUser(memberDetails);
		validateUpdateAuthorization(user, storeId);

		Store store = storeRepository.findByStoreIdAndDeletedFalse(storeId)
			.orElseThrow(StoreNotFoundException::new);

		store.markAsDeleted();
		storeRepository.save(store);

		return "Store ID " + storeId + " 삭제되었습니다.";
	}

	@Transactional
	public Boolean toggleActive(Long storeId) {
		Store store = storeRepository.findById(storeId)
			.orElseThrow(StoreNotFoundException::new);

		store.toggleActive();
		return store.getIsActive();
	}

	private void validateViewAuthorization(User user, Long storeId) {
		if (!(Role.SUPER_ADMIN.equals(user.getRole())
			  || (Role.MANAGER.equals(user.getRole()) && storeId.equals(user.getStoreId())))) {
			throw new StoreViewUnauthorizedException();
		}
	}

	private void validateUpdateAuthorization(User user, Long storeId) {
		if (!(Role.SUPER_ADMIN.equals(user.getRole())
			  || (Role.MANAGER.equals(user.getRole()) && storeId.equals(user.getStoreId())))) {
			throw new StoreUpdateUnauthorizedException();
		}
	}

	private User getUser(MemberDetails memberDetails) {
		return userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
	}
}
