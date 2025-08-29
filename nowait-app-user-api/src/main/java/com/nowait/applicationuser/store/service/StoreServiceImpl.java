package com.nowait.applicationuser.store.service;

import java.awt.print.Book;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

import com.nowait.applicationuser.reservation.repository.WaitingUserRedisRepository;
import com.nowait.applicationuser.store.dto.StoreDepartmentReadResponse;
import com.nowait.applicationuser.store.dto.StoreDetailReadResponse;
import com.nowait.applicationuser.store.dto.StoreImageUploadResponse;
import com.nowait.applicationuser.store.dto.StorePageReadResponse;
import com.nowait.applicationuser.store.dto.StoreSearchResponse;
import com.nowait.applicationuser.store.dto.StoreWaitingInfo;
import com.nowait.domaincorerdb.department.entity.Department;
import com.nowait.domaincorerdb.department.repository.DepartmentRepository;
import com.nowait.domaincorerdb.store.entity.ImageType;
import com.nowait.domaincorerdb.store.entity.Store;
import com.nowait.domaincorerdb.store.entity.StoreImage;
import com.nowait.domaincorerdb.store.exception.StoreNotFoundException;
import com.nowait.domaincorerdb.store.exception.StoreParamEmptyException;
import com.nowait.domaincorerdb.store.repository.StoreImageRepository;
import com.nowait.domaincorerdb.store.repository.StoreRepository;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domaincorerdb.user.exception.UserNotFoundException;
import com.nowait.domaincorerdb.user.repository.UserRepository;
import com.nowait.domaincoreredis.common.util.RedisKeyUtils;
import com.nowait.domainuserrdb.bookmark.entity.Bookmark;
import com.nowait.domainuserrdb.bookmark.exception.BookmarkNotFoundException;
import com.nowait.domainuserrdb.bookmark.repository.BookmarkRepository;
import com.nowait.domainuserrdb.oauth.dto.CustomOAuth2User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

	private final StoreRepository storeRepository;
	private final StoreImageRepository storeImageRepository;
	private final DepartmentRepository departmentRepository;
	private final StringRedisTemplate redisTemplate;
	private final BookmarkRepository bookmarkRepository;
	private final WaitingUserRedisRepository waitingRepo;

	@Override
	@Transactional(readOnly = true)
	public StoreDepartmentReadResponse getAllStoresByPageAndDeparments(Pageable pageable,
		CustomOAuth2User customOAuth2User) {

		User user = customOAuth2User.getUser();

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

		// 5) 북마크 조회
		Collection<Bookmark> allBookmarks = bookmarkRepository.findAllByUserAndDeletedFalse(user);
		Map<Long, Long> bookmarkIdMap = allBookmarks.stream()
			.collect(Collectors.toMap(
				b -> b.getStore().getStoreId(),
				Bookmark::getId
			));

		// 5) Dto 매핑
		List<StorePageReadResponse> content = stores.stream()
			.map(store -> {
				Long bookmarkId = bookmarkIdMap.get(store.getStoreId());
				boolean isBookmark = bookmarkId != null;
				List<StoreImageUploadResponse> imgs = imageMap.getOrDefault(store.getStoreId(), List.of());
				String departmentName = deptNameMap.getOrDefault(store.getDepartmentId(), "Unknown Department");
				Long waitingCount = waitingSizeMap.getOrDefault(store.getStoreId(), 0L);

				return StorePageReadResponse.fromEntity(store, bookmarkId, imgs, departmentName, waitingCount, isBookmark);
			})
			.toList();

		boolean hasNext = slice.hasNext();

		return StoreDepartmentReadResponse.of(content, hasNext);
	}

	@Override
	@Transactional(readOnly = true)
	public StoreDetailReadResponse getStoreByPublicCode(String publicCode, CustomOAuth2User customOAuth2User) {

		if (publicCode == null)
			throw new StoreParamEmptyException();
		User user = customOAuth2User.getUser();

		Store store = storeRepository.findByPublicCodeAndDeletedFalse(publicCode)
			.orElseThrow(StoreNotFoundException::new);

		Long storeId = store.getStoreId();;

		String departmentName = departmentRepository.findById(store.getDepartmentId())
			.map(Department::getName)
			.orElse("Unknown Department");

		Optional<Bookmark> bookmark = bookmarkRepository.findByUserAndStoreAndDeletedFalse(user, store);
		boolean isBookmark = bookmark.isPresent();
		Long bookmarkId = isBookmark ? bookmark.get().getId() : null;

		// 2-1) Redis에서 각 Store의 웨이팅 사이즈 조회
		String key = "waiting:" + storeId;
		long waitingSize = redisTemplate.opsForZSet().zCard(key);
		boolean userWaiting = waitingRepo.isUserWaiting(storeId, String.valueOf(customOAuth2User.getUserId()));

		List<StoreImage> images = storeImageRepository.findByStore(store);
		List<StoreImageUploadResponse> imageDto = images.stream()
			.map(StoreImageUploadResponse::fromEntity)
			.toList();

		return StoreDetailReadResponse.fromEntity(store, bookmarkId, imageDto, departmentName, waitingSize, isBookmark,
			userWaiting);
	}

	@Override
	public List<StoreSearchResponse> searchByKeywordNative(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			throw new StoreParamEmptyException();
		}

		// 1) 페이징된 Store 스냅샷 조회
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
				return StoreSearchResponse.fromEntity(store, imgs, departmentName, waitingCount);
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
				if (type == null || !"zset".equals(type.code()))
					continue;

				Long count = redisTemplate.opsForZSet().zCard(key);
				String storeId = key.replace(PREFIX, "");

				// DB에서 storeName 조회
				String storeName = storeRepository.findById(Long.valueOf(storeId))
					.map(Store::getName)
					.orElse(UNKNOWN_STORE_NAME);

				Store store = storeRepository.findById(Long.valueOf(storeId))
					.orElseThrow(StoreNotFoundException::new);
				Department department = departmentRepository.getReferenceById(store.getDepartmentId());
				List<StoreImage> storeImageList = storeImageRepository.findByStoreAndImageType(store, ImageType.BANNER);

				String imageUrl = storeImageList.isEmpty() ? null : storeImageList.get(0).getImageUrl();

				result.add(new StoreWaitingInfo(
					imageUrl,
					department.getName(),
					storeId,
					storeName,
					count != null ? count : 0
				));
			}
		}

		// 정렬 로직: 대기 인원 기준 desc/asc
		Comparator<StoreWaitingInfo> comparator = Comparator.comparing(StoreWaitingInfo::getWaitingCount);
		if (desc)
			comparator = comparator.reversed();
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
	public List<StorePageReadResponse> getAllStoresByPageAndDeparments(List<Long> storeIds,
		Map<Long, Long> bookmarkMap) {
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
		List<StorePageReadResponse> content = stores.stream()
			.map(store -> {
				Long bookmarkId = bookmarkMap.get(store.getStoreId());
				boolean isBookmark = bookmarkId != null;

				List<StoreImageUploadResponse> imgs = imageMap
					.getOrDefault(store.getStoreId(), List.of());
				String departmentName = deptNameMap
					.getOrDefault(store.getDepartmentId(), "Unknown Department");
				Long waitingCount =
					waitingSizeMap.getOrDefault(store.getStoreId(), 0L);

				return StorePageReadResponse.fromEntity(store, bookmarkId, imgs, departmentName, waitingCount,
					isBookmark);
			})
			.toList();

		return content;
	}
}
