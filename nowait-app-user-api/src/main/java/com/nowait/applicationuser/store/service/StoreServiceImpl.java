package com.nowait.applicationuser.store.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationuser.store.dto.StoreDepartmentReadResponse;
import com.nowait.applicationuser.store.dto.StoreImageUploadResponse;
import com.nowait.applicationuser.store.dto.StorePageReadDto;
import com.nowait.applicationuser.store.dto.StoreWaitingInfo;
import com.nowait.domaincorerdb.department.entity.Department;
import com.nowait.domaincorerdb.department.repository.DepartmentRepository;
import com.nowait.domaincorerdb.store.entity.Store;
import com.nowait.domaincorerdb.store.entity.StoreImage;
import com.nowait.domaincorerdb.store.exception.StoreNotFoundException;
import com.nowait.domaincorerdb.store.exception.StoreParamEmptyException;
import com.nowait.domaincorerdb.store.repository.StoreImageRepository;
import com.nowait.domaincorerdb.store.repository.StoreRepository;
import com.nowait.domaincoreredis.common.util.RedisKeyUtils;

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

		// 2-1) Redis에서 각 Store의 웨이팅 사이즈 조회
		Map<Long, Long> waitingSizeMap = storeIds.stream()
			 .collect(Collectors.toMap(
				Function.identity(),
				 storeId -> {
					 String key = "waiting:" + storeId;
					 try {
						 return redisTemplate.opsForZSet().zCard(key);
					 } catch (Exception e) {
						 return 0L; // Redis 접근 실패 시 0으로 처리
					 }

				 }
			 ));

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
				Long waitingCount =
					waitingSizeMap.getOrDefault(store.getStoreId(), 0L);
				return StorePageReadDto.fromEntity(store, imgs, departmentName, waitingCount);
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

		// 2-1) Redis에서 각 Store의 웨이팅 사이즈 조회
		String key = "waiting:" + storeId;
		Long waitingSize = 0L;
		try {
			redisTemplate.opsForZSet().zCard(key);
		} catch (Exception e) {
			waitingSize = 0L; // Redis 접근 실패 시 0으로 처리
		}

		List<StoreImage> images = storeImageRepository.findByStore(store);
		List<StoreImageUploadResponse> imageDto = images.stream()
			.map(StoreImageUploadResponse::fromEntity)
			.toList();

		return StorePageReadDto.fromEntity(store, imageDto, departmentName, waitingSize);
	}

	@Override
	public List<StorePageReadDto> searchByKeywordNative(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			throw new StoreParamEmptyException();
		}

		// 1) 페이징된 Store 스냅샷 조회
		// Like 사용
		// List<Store> stores = storeRepository.findByNameContainingIgnoreCaseAndDeletedFalse(keyword);
		// 풀텍스트인덱스 사용
		List<Store> stores = storeRepository.searchByKeywordNative(keyword);

		// 2) 각 StoreId / Department ID 추출
		List<Long> storeIds = stores.stream()
			.map(Store::getStoreId)
			.toList();
		List<Long> deptIds = stores.stream()
			.map(Store::getDepartmentId)
			.distinct()
			.toList();

		// 2-1) Redis에서 각 Store의 웨이팅 사이즈 조회
		Map<Long, Long> waitingSizeMap = storeIds.stream()
			.collect(Collectors.toMap(
				Function.identity(),
				storeId -> {
					String key = "waiting:" + storeId;
					try {
						return redisTemplate.opsForZSet().zCard(key);
					} catch (Exception e) {
						return 0L; // Redis 접근 실패 시 0으로 처리
					}
				}
			));

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
				Long waitingCount =
					waitingSizeMap.getOrDefault(store.getStoreId(), 0L);
				return StorePageReadDto.fromEntity(store, imgs, departmentName, waitingCount);
			})
			.toList();
	}

	// 주점 대기 리스트 반환 (많은 순/적은 순)
	@Transactional(readOnly = true)
	public List<StoreWaitingInfo> getStoresByWaitingCount(boolean desc) {
		final String PREFIX = RedisKeyUtils.buildWaitingKeyPrefix(); // 예: "waiting:"
		final String UNKNOWN_STORE_NAME = "UNKNOWN";

		RedisConnection connection = getSafeConnection(); // 안전하게 Redis 커넥션 획득

		ScanOptions options = ScanOptions.scanOptions()
			.match(PREFIX + "[0-9]*") // 숫자로 끝나는 waiting key만 매칭
			.count(1000)
			.build();

		List<StoreWaitingInfo> result = new ArrayList<>();

		// 반드시 try-with-resources로 Cursor 닫아줘야 함
		try (Cursor<byte[]> cursor = connection.scan(options)) {
			while (cursor.hasNext()) {
				String key = new String(cursor.next(), StandardCharsets.UTF_8);

				// zset 타입인지 확인
				DataType type = redisTemplate.type(key);
				if (type == null || !"zset".equals(type.code())) continue;

				Long count = redisTemplate.opsForZSet().zCard(key);
				String storeId = key.replace(PREFIX, "");

				// DB에서 storeName 조회
				String storeName = storeRepository.findById(Long.valueOf(storeId))
					.map(Store::getName)
					.orElse(UNKNOWN_STORE_NAME);

				result.add(new StoreWaitingInfo(storeId, storeName, count != null ? count : 0));
			}
		}

		// 정렬 로직: 대기 인원 기준 desc/asc
		Comparator<StoreWaitingInfo> comparator = Comparator.comparing(StoreWaitingInfo::getWaitingCount);
		if (desc) comparator = comparator.reversed();
		result.sort(comparator);

		return result.stream()
			.limit(5)
			.toList();
	}

	private RedisConnection getSafeConnection() {
		RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
		if (factory == null) {
			throw new IllegalStateException("RedisConnectionFactory is not configured");
		}
		return factory.getConnection();
	}


	@Override
	@Transactional(readOnly = true)
	public List<StorePageReadDto> getAllStoresByPageAndDeparments(List<Long> storeIds) {
		// 1) 페이징된 Store 스냅샷 조회
		List<Store> stores = storeRepository.findAllByStoreIdInOrderByStoreIdAsc(storeIds);

		// 2) 각 StoreId / Department ID 추출
		List<Long> deptIds = stores.stream()
			.map(Store::getDepartmentId)
			.distinct()
			.toList();

		// 2-1) Redis에서 각 Store의 웨이팅 사이즈 조회
		Map<Long, Long> waitingSizeMap = storeIds.stream()
			.collect(Collectors.toMap(
				Function.identity(),
				storeId -> {
					String key = "waiting:" + storeId;
					try {
						return redisTemplate.opsForZSet().zCard(key);
					} catch (Exception e) {
						return 0L; // Redis 접근 실패 시 0으로 처리
					}

				}
			));

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
				Long waitingCount =
					waitingSizeMap.getOrDefault(store.getStoreId(), 0L);
				return StorePageReadDto.fromEntity(store, imgs, departmentName, waitingCount);
			})
			.toList();

		return content;
	}
}
