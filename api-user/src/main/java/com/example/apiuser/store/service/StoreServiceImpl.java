package com.example.apiuser.store.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.apiuser.store.dto.StoreImageUploadResponse;
import com.example.apiuser.store.dto.StoreReadDto;
import com.example.apiuser.store.dto.StoreReadResponse;
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
	@Transactional(readOnly = true)
	public StoreReadResponse getAllStores() {
		List<Store> stores = storeRepository.findAllByDeletedFalse();

		List<StoreReadDto> storeRead = stores.stream()
			.map(store -> {
				List<StoreImage> images = storeImageRepository.findByStore(store);
				List<StoreImageUploadResponse> imageDto = images.stream()
					.map(StoreImageUploadResponse::fromEntity)
					.toList();
				return StoreReadDto.fromEntity(store, imageDto);
			})
			.toList();

		boolean hasNext = false;

		return StoreReadResponse.of(storeRead, hasNext);
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
	public List<StoreReadDto> searchStoresByName(String name) {
		List<Store> stores = storeRepository.findByNameContainingIgnoreCaseAndDeletedFalse(name);
		return stores.stream()
			.map(store -> {
				List<StoreImage> images = storeImageRepository.findByStore(store);
				List<StoreImageUploadResponse> imageDto = images.stream()
					.map(StoreImageUploadResponse::fromEntity)
					.toList();
				return StoreReadDto.fromEntity(store, imageDto);
			})
			.toList();
	}
}
