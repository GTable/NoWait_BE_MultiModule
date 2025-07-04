package com.nowait.applicationuser.store.service;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationuser.store.dto.StoreImageUploadResponse;
import com.nowait.applicationuser.store.dto.StoreReadDto;
import com.nowait.applicationuser.store.dto.StoreReadResponse;
import com.nowait.domaincorerdb.store.entity.Store;
import com.nowait.domaincorerdb.store.entity.StoreImage;
import com.nowait.domaincorerdb.store.exception.StoreNotFoundException;
import com.nowait.domaincorerdb.store.exception.StoreParamEmptyException;
import com.nowait.domaincorerdb.store.repository.StoreImageRepository;
import com.nowait.domaincorerdb.store.repository.StoreRepository;

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
	public StoreReadResponse getAllStoresByPage(Pageable pageable) {
		Slice<Store> stores = storeRepository.findAllByDeletedFalseOrderByStoreIdAsc(pageable);

		List<StoreReadDto> storeRead = stores.getContent().stream()
			.map(store -> {
				List<StoreImage> images = storeImageRepository.findByStore(store);
				List<StoreImageUploadResponse> imageDto = images.stream()
					.map(StoreImageUploadResponse::fromEntity)
					.toList();
				return StoreReadDto.fromEntity(store, imageDto);
			})
			.toList();

		boolean hasNext = stores.hasNext();

		return StoreReadResponse.of(storeRead, hasNext);
	}

	@Override
	@Transactional(readOnly = true)
	public StoreReadDto getStoreByStoreId(Long storeId) {
		if (storeId == null) throw new StoreParamEmptyException();

		Store store = storeRepository.findByStoreIdAndDeletedFalse(storeId)
			.orElseThrow(StoreNotFoundException::new);

		List<StoreImage> images = storeImageRepository.findByStore(store);
		List<StoreImageUploadResponse> imageDto = images.stream()
			.map(StoreImageUploadResponse::fromEntity)
			.toList();

		return StoreReadDto.fromEntity(store, imageDto);
	}

	@Override
	public List<StoreReadDto> searchStoresByName(String name) {
		if (name == null || name.isBlank()) {
			throw new StoreParamEmptyException();
		}

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
