package com.nowait.applicationuser.store.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationuser.store.dto.StoreDepartmentReadResponse;
import com.nowait.applicationuser.store.dto.StoreImageUploadResponse;
import com.nowait.applicationuser.store.dto.StorePageReadDto;
import com.nowait.applicationuser.store.dto.StoreReadDto;
import com.nowait.applicationuser.store.dto.StoreReadResponse;
import com.nowait.applicationuser.store.dto.StoreWaitingInfo;
import com.nowait.domaincorerdb.department.entity.Department;
import com.nowait.domaincorerdb.department.repository.DepartmentRepository;
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
	private final DepartmentRepository departmentRepository;
	private final StringRedisTemplate redisTemplate;


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
	public StoreDepartmentReadResponse getAllStoresByPageAndDeparments(Pageable pageable) {
		// 1) 페이징된 Store 스냅샷 조회
		Slice<Store> slice = storeRepository.findAllByDeletedFalseOrderByStoreIdAsc(pageable);
		List<Store> stores = slice.getContent();

		// 2) 각 StoreId / Department ID 추출
		List<Long> storeIds = stores.stream()
			.map(Store::getStoreId)
			.toList();
		List<Long> deptIds = stores.stream()
			.map(Store::getDepartmentId)
			.distinct()
			.toList();

		// 3) 각 StoreId에 해당하는 이미지 조회
		List<StoreImage> allImages = storeImageRepository.findByStore_StoreIdIn(storeIds);
		Map<Long, List<StoreImageUploadResponse>> imageMap = allImages.stream()
			.map(StoreImageUploadResponse::fromEntity)
			.collect(Collectors.groupingBy(
				StoreImageUploadResponse::getStoreId
			));


		// 4) 각 DepartmentId에 해당하는 이름 조회
		List<Department> allDepts = departmentRepository.findAllById(deptIds);
		Map<Long, String> deptNameMap = allDepts.stream()
			.collect(Collectors.toMap(
				Department::getId,
				Department::getName
			));

		// 5) Dto 매핑
		List<StorePageReadDto> content = stores.stream()
			.map(store -> {
				List<StoreImageUploadResponse> imgs = imageMap
					.getOrDefault(store.getStoreId(), List.of());
				String departmentName = deptNameMap
					.getOrDefault(store.getDepartmentId(), "Unknown Department");
				return StorePageReadDto.fromEntity(store, imgs, departmentName);
			})
			.toList();

		boolean hasNext = slice.hasNext();

		return StoreDepartmentReadResponse.of(content, hasNext);
	}

	@Override
	@Transactional(readOnly = true)
	public StorePageReadDto getStoreByStoreId(Long storeId) {
		if (storeId == null) throw new StoreParamEmptyException();

		Store store = storeRepository.findByStoreIdAndDeletedFalse(storeId)
			.orElseThrow(StoreNotFoundException::new);

		String departmentName = departmentRepository.findById(store.getDepartmentId())
			.map(Department::getName)
			.orElse("Unknown Department");

		List<StoreImage> images = storeImageRepository.findByStore(store);
		List<StoreImageUploadResponse> imageDto = images.stream()
			.map(StoreImageUploadResponse::fromEntity)
			.toList();

		return StorePageReadDto.fromEntity(store, imageDto, departmentName);
	}

	@Override
	public List<StorePageReadDto> searchStoresByName(String name) {
		if (name == null || name.isBlank()) {
			throw new StoreParamEmptyException();
		}

		// 1) 페이징된 Store 스냅샷 조회
		List<Store> stores = storeRepository.findByNameContainingIgnoreCaseAndDeletedFalse(name);

		// 2) 각 StoreId / Department ID 추출
		List<Long> storeIds = stores.stream()
			.map(Store::getStoreId)
			.toList();
		List<Long> deptIds = stores.stream()
			.map(Store::getDepartmentId)
			.distinct()
			.toList();

		// 3) 각 StoreId에 해당하는 이미지 조회
		List<StoreImage> allImages = storeImageRepository.findByStore_StoreIdIn(storeIds);
		Map<Long, List<StoreImageUploadResponse>> imageMap = allImages.stream()
			.map(StoreImageUploadResponse::fromEntity)
			.collect(Collectors.groupingBy(
				StoreImageUploadResponse::getStoreId
			));

		// 4) 각 DepartmentId에 해당하는 이름 조회
		List<Department> allDepts = departmentRepository.findAllById(deptIds);
		Map<Long, String> deptNameMap = allDepts.stream()
			.collect(Collectors.toMap(
				Department::getId,
				Department::getName
			));

		// 5) Dto 매핑
		return stores.stream()
			.map(store -> {
				List<StoreImageUploadResponse> imgs = imageMap
					.getOrDefault(store.getStoreId(), List.of());
				String departmentName = deptNameMap
					.getOrDefault(store.getDepartmentId(), "Unknown Department");
				return StorePageReadDto.fromEntity(store, imgs, departmentName);
			})
			.toList();
	}

	// 주점 대기 리스트 반환 (많은 순/적은 순)
	@Transactional(readOnly = true)
	public List<StoreWaitingInfo> getStoresByWaitingCount(boolean desc) {
		// 1. 모든 waiting:{storeId} key 조회 (패턴 탐색)
		Set<String> keys = redisTemplate.keys("waiting:*");
		if (keys == null) return List.of();

		List<StoreWaitingInfo> result = keys.stream()
			.filter(key -> key.startsWith("waiting:") && !key.startsWith("waiting:party:"))
			.filter(key -> "zset".equals(redisTemplate.type(key).code())) // 타입 확인 확실히!
			.map(key -> {
				Long count = redisTemplate.opsForZSet().zCard(key);
				String storeId = key.replace("waiting:", "");
				String storeName = storeRepository.findById(Long.valueOf(storeId))
					.map(Store::getName)
					.orElse("UNKNOWN");
				return new StoreWaitingInfo(storeId, storeName, count != null ? count : 0);
			})
			.sorted((a, b) -> b.getWaitingCount().compareTo(a.getWaitingCount()))
			.toList();

		return result;
	}
}
