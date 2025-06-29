package com.example.apiadmin.store.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.apiadmin.store.dto.StoreCreateRequest;
import com.example.apiadmin.store.dto.StoreCreateResponse;
import com.example.apiadmin.store.dto.StoreImageUploadResponse;
import com.example.apiadmin.store.dto.StoreReadDto;
import com.example.apiadmin.store.dto.StoreUpdateRequest;
import com.example.domainstore.entity.Store;
import com.example.domainstore.entity.StoreImage;
import com.example.domainstore.repository.StoreImageRepository;
import com.example.domainstore.repository.StoreRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

	private final StoreRepository storeRepository;
	private final StoreImageRepository storeImageRepository;

	@Override
	@Transactional
	public StoreCreateResponse createStore(StoreCreateRequest request) {
		Store toSave = request.toEntity();

		Store saved = storeRepository.save(toSave);

		return StoreCreateResponse.fromEntity(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public StoreReadDto getStoreByStoreId(Long storeId) {
		Store store = storeRepository.findByStoreIdAndDeletedFalse(storeId)
			.orElseThrow(() -> new EntityNotFoundException(storeId + " store not found."));

		List<StoreImage> images = storeImageRepository.findByStore(store);
		List<StoreImageUploadResponse> imageDto = images.stream()
			.map(StoreImageUploadResponse::fromEntity)
			.toList();

		return StoreReadDto.fromEntity(store, imageDto);
	}

	@Override
	@Transactional
	public StoreReadDto updateStore(Long storeId, StoreUpdateRequest request) {
		Store store = storeRepository.findByStoreIdAndDeletedFalse(storeId)
			.orElseThrow(() -> new EntityNotFoundException(storeId + " store not found."));

		store.updateInfo(
			request.getName(),
			request.getLocation(),
			request.getDescription()
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
	public String deleteStore(Long storeId) {
		Store store = storeRepository.findByStoreIdAndDeletedFalse(storeId)
			.orElseThrow(() -> new EntityNotFoundException(storeId + " store not found."));

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
