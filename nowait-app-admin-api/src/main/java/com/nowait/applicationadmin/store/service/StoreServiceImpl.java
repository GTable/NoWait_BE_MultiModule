package com.nowait.applicationadmin.store.service;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationadmin.store.dto.StoreCreateRequest;
import com.nowait.applicationadmin.store.dto.StoreCreateResponse;
import com.nowait.applicationadmin.store.dto.StoreImageUploadResponse;
import com.nowait.applicationadmin.store.dto.StoreReadDto;
import com.nowait.applicationadmin.store.dto.StoreUpdateRequest;
import com.nowait.common.enums.Role;
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
	public StoreReadDto getStoreByStoreId(Long storeId, MemberDetails memberDetails) {
		if (storeId == null) throw new StoreParamEmptyException();
		User user = userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
		if (!Role.SUPER_ADMIN.equals(user.getRole()) && !user.getStoreId().equals(storeId)) {
			throw new StoreViewUnauthorizedException();
		}
		Store store = storeRepository.findByStoreIdAndDeletedFalse(storeId)
			.orElseThrow(StoreNotFoundException::new);

		List<StoreImage> images = storeImageRepository.findByStore(store);
		List<StoreImageUploadResponse> imageDto = images.stream()
			.map(StoreImageUploadResponse::fromEntity)
			.toList();

		return StoreReadDto.fromEntity(store, imageDto);
	}

	@Override
	@Transactional
	public StoreReadDto updateStore(Long storeId, StoreUpdateRequest request, MemberDetails memberDetails) {
		User user = userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
		if (storeId == null || request == null) throw new StoreParamEmptyException();
		if (!Role.SUPER_ADMIN.equals(user.getRole()) && !user.getStoreId().equals(storeId)) {
			throw new StoreUpdateUnauthorizedException();
		}
		Store store = storeRepository.findByStoreIdAndDeletedFalse(storeId)
			.orElseThrow(StoreNotFoundException::new);

		store.updateInfo(
			request.getName(),
			request.getLocation(),
			request.getDescription(),
			request.getNotice(),
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
		User user = userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
		if (storeId == null) {
			throw new StoreParamEmptyException();
		}
		if (!Role.SUPER_ADMIN.equals(user.getRole()) && !user.getStoreId().equals(storeId)) {
			throw new StoreDeleteUnauthorizedException();
		}

		Store store = storeRepository.findByStoreIdAndDeletedFalse(storeId)
			.orElseThrow(StoreNotFoundException::new);

		store.markAsDeleted();
		storeRepository.save(store);

		return "Store ID " + storeId + " 삭제되었습니다.";
	}

	@Transactional
	public Boolean toggleActive(Long storeId) {
		Store store = storeRepository.findById(storeId)
			.orElseThrow(() -> new IllegalArgumentException("해당 스토어가 존재하지 않습니다."));

		store.toggleActive();
		return store.getIsActive();
	}
}
