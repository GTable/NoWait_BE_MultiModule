package com.nowait.applicationuser.reservation.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationuser.reservation.dto.MyWaitingQueueDto;
import com.nowait.applicationuser.reservation.dto.ReservationCreateRequestDto;
import com.nowait.applicationuser.reservation.dto.ReservationCreateResponseDto;
import com.nowait.applicationuser.reservation.dto.WaitingResponseDto;
import com.nowait.applicationuser.reservation.dto.WaitingSnapshot;
import com.nowait.applicationuser.reservation.repository.WaitingUserRedisRepository;
import com.nowait.common.enums.ReservationStatus;
import com.nowait.common.enums.Role;
import com.nowait.domaincorerdb.department.entity.Department;
import com.nowait.domaincorerdb.department.repository.DepartmentRepository;
import com.nowait.domaincorerdb.reservation.entity.Reservation;
import com.nowait.domaincorerdb.reservation.exception.DuplicateReservationException;
import com.nowait.domaincorerdb.reservation.exception.ReservationAddUnauthorizedException;
import com.nowait.domaincorerdb.reservation.exception.ReservationNotFoundException;
import com.nowait.domaincorerdb.reservation.exception.ReservationNumberIssueFailException;
import com.nowait.domaincorerdb.reservation.repository.ReservationRepository;
import com.nowait.domaincorerdb.store.entity.ImageType;
import com.nowait.domaincorerdb.store.entity.Store;
import com.nowait.domaincorerdb.store.entity.StoreImage;
import com.nowait.domaincorerdb.store.exception.StoreNotFoundException;
import com.nowait.domaincorerdb.store.exception.StoreWaitingDisabledException;
import com.nowait.domaincorerdb.store.repository.StoreImageRepository;
import com.nowait.domaincorerdb.store.repository.StoreRepository;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domaincorerdb.user.exception.UserNotFoundException;
import com.nowait.domaincorerdb.user.repository.UserRepository;
import com.nowait.domaincoreredis.common.util.RedisKeyUtils;
import com.nowait.domaincoreredis.reservation.exception.UserWaitingLimitExceededException;
import com.nowait.domaincoreredis.reservation.repository.WaitingPermitLuaRepository;
import com.nowait.domainuserrdb.oauth.dto.CustomOAuth2User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationService {

	private final ReservationRepository reservationRepository;
	private final StoreRepository storeRepository;
	private final UserRepository userRepository;
	private final WaitingUserRedisRepository waitingUserRedisRepository;
	private final DepartmentRepository departmentRepository;
	private final StoreImageRepository storeImageRepository;
	private final WaitingPermitLuaRepository waitingPermitLuaRepository;
	private final RedisTemplate redisTemplate;
	private static final int USER_LIMIT = 3;
	private static final long LEASE_MS = 30_000; // 20~60초 권장

	/**
	 * 웨이팅 등록 기존 로직
	 */
	// public WaitingResponseDto registerWaiting(
	// 	Long storeId, CustomOAuth2User customOAuth2User, ReservationCreateRequestDto requestDto
	// ) {
	// 	// Store 유효성 검증 추가
	// 	Store store = storeRepository.findById(storeId)
	// 		.orElseThrow(StoreNotFoundException::new);
	// 	if (Boolean.FALSE.equals(store.getIsActive()))
	// 		throw new StoreWaitingDisabledException();
	//
	// 	// User Role 검증 추가
	// 	User user = userRepository.findById(customOAuth2User.getUserId())
	// 		.orElseThrow(UserNotFoundException::new);
	// 	if (user.getRole() == Role.MANAGER) {
	// 		throw new IllegalArgumentException("Manager cannot register waiting");
	// 	}
	//
	// 	String userId = customOAuth2User.getUserId().toString();
	// 	long timestamp = System.currentTimeMillis();
	//
	// 	// 예약 신청 유저 큐(queue)에 추가
	// 	String reservationId = waitingUserRedisRepository.addToWaitingQueue(storeId, userId, requestDto.getPartySize(),
	// 		timestamp);
	// 	if (reservationId == null) {
	// 		throw new IllegalStateException("예약 번호 발급 실패");
	// 	}
	//
	// 	// 신규 등록/기존 등록 관계없이 내 순번, 전체 인원 반환
	// 	Long rank = waitingUserRedisRepository.getRank(storeId, userId);
	// 	return WaitingResponseDto.builder()
	// 		.reservationNumber(reservationId)
	// 		.rank(rank == null ? -1 : rank.intValue() + 1)
	// 		.partySize(requestDto.getPartySize() == null ? 0 : requestDto.getPartySize())
	// 		.build();
	// }
	public WaitingResponseDto registerWaiting(Long storeId, CustomOAuth2User principal,
		ReservationCreateRequestDto dto) {

		// 0) 스토어/유저 검증 복구
		Store store = storeRepository.findById(storeId).orElseThrow(StoreNotFoundException::new);
		if (Boolean.FALSE.equals(store.getIsActive()))
			throw new StoreWaitingDisabledException();

		User user = userRepository.findById(principal.getUserId()).orElseThrow(UserNotFoundException::new);
		if (user.getRole() == Role.MANAGER)
			throw new ReservationAddUnauthorizedException();

		// (기존 유효성 검사 동일)
		String userId = user.getId().toString();
		Duration ttlTo3am = waitingUserRedisRepository.calculateTTLUntilNext03AM();

		// 1) 임대 획득
		String token = UUID.randomUUID().toString();
		int attempts = 0;
		while (true) {
			boolean ok = waitingPermitLuaRepository.acquireLease(userId, token, System.currentTimeMillis(), LEASE_MS,
				USER_LIMIT, ttlTo3am);
			if (ok)
				break;
			if (++attempts >= 3)
				throw new UserWaitingLimitExceededException();
			try {
				Thread.sleep((long)(5 * Math.pow(3, attempts - 1)));
			} catch (InterruptedException ignored) {
			}
		}

		// 2) 임대 획득 후 중복 체크
		WaitingSnapshot existingSnapshot = waitingUserRedisRepository.getWaitingSnapshot(storeId, userId);
		if (existingSnapshot != null &&  existingSnapshot.getRank() != null) {
			waitingPermitLuaRepository.releaseLease(userId, token);
			throw new DuplicateReservationException();
		}

		WaitingSnapshot snapshot = null;
		try {
			// 2) 스토어 큐 등록(기존 메서드 그대로)
			long ts = System.currentTimeMillis();

			snapshot = waitingUserRedisRepository.addToWaitingQueueLua(
				storeId, userId, dto.getPartySize(), ts, ttlTo3am
			);

			if (snapshot == null || snapshot.getReservationId() == null)
				throw new ReservationNumberIssueFailException();


			if (snapshot.isNew()) {
				waitingPermitLuaRepository.finalizeActive(
					userId,
					token,
					String.valueOf(storeId),
					snapshot.getReservationId(),
					ttlTo3am
				);
			} else {
				// Lua 스크립트 중복 감지 케이스
				waitingPermitLuaRepository.releaseLease(userId, token);
				throw new DuplicateReservationException();
			}

			// 3) 확정(holding → active)
			WaitingSnapshot after = waitingUserRedisRepository.getWaitingSnapshot(storeId, userId);
			if (after == null)
				throw new ReservationNumberIssueFailException();

			// 4) 응답
			return WaitingResponseDto.builder()
				.reservationNumber(after.getReservationId())
				.rank(after.getRank() == null ? -1 : after.getRank().intValue() + 1)
				.partySize(after.getPartySize() == null ? 0 : after.getPartySize())
				.build();

		} catch (RuntimeException e) {
			// 실패 시 임대 반납
			if (snapshot == null || (snapshot.isNew())) {
				waitingPermitLuaRepository.releaseLease(userId, token);
			}
			throw e;
		}
	}

	public WaitingResponseDto myWaitingInfo(Long storeId, CustomOAuth2User customOAuth2User) {
		String userId = customOAuth2User.getUserId().toString();
		// 입력 검증 추가
		if (storeId == null || userId.trim().isEmpty()) {
			throw new IllegalArgumentException("Invalid storeId or userId");
		}

		Long rank = waitingUserRedisRepository.getRank(storeId, userId);
		Integer partySize = waitingUserRedisRepository.getPartySize(storeId, userId);
		String reservationId = waitingUserRedisRepository.getReservationId(storeId, userId);

		if (reservationId == null) {
			throw new ReservationNotFoundException();
		}

		return WaitingResponseDto.builder()
			.reservationNumber(reservationId)
			.rank(rank == null ? -1 : rank.intValue() + 1)
			.partySize(partySize == null ? 0 : partySize)
			.build();
	}

	public boolean cancelWaiting(Long storeId, CustomOAuth2User customOAuth2User) {
		String userId = customOAuth2User.getUserId().toString();
		if (storeId == null) {
			throw new StoreNotFoundException();
		}

		if (userId.trim().isEmpty()) {
			throw new UserNotFoundException();
		}

		String reservationNumber = waitingUserRedisRepository.getReservationId(storeId, userId);
		if (reservationNumber == null) {
			throw new ReservationNotFoundException();
		}
		Integer partySize = waitingUserRedisRepository.getPartySize(storeId, userId);
		Long ts = waitingUserRedisRepository.getWaitingTimestamp(storeId, userId);


		// 대기열에서 제거 및 결과 반환
		if (reservationRepository.existsReservationByReservationNumber(reservationNumber)) {
			Reservation reservation = Reservation.builder()
				.reservationNumber(reservationNumber)
				.partySize(partySize)
				.status(ReservationStatus.CANCELLED)
				.store(storeRepository.getReferenceById(storeId))
				.user(userRepository.getReferenceById(Long.parseLong(userId)))
				.updatedAt(LocalDateTime.now())
				.requestedAt(ts != null ? LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.of("Asia/Seoul"))
					: LocalDateTime.now())
				.build();

			reservationRepository.save(reservation);
		}

		waitingUserRedisRepository.removeWaiting(storeId, userId);
		waitingPermitLuaRepository.removeActiveMember(userId, String.valueOf(storeId), reservationNumber);

		return true;
	}

	//TODO 성능 개선 필요
	public List<MyWaitingQueueDto> getAllMyWaitings(CustomOAuth2User customOAuth2User) {
		String userId = customOAuth2User.getUserId().toString();

		Set<String> members = waitingPermitLuaRepository.getActiveMembers(userId);
		if (members.isEmpty())
			return Collections.emptyList();

		Map<Long, String> activeResIdByStore = members.stream()
			.map(m -> m.split(":", 2))
			.filter(a -> a.length == 2)
			.collect(Collectors.toMap(a -> Long.parseLong(a[0]), a -> a[1], (a, b) -> a));

		// 1) 현재 SCAN 기반으로 얻어온 storeId 리스트
		// List<Long> storeIds = waitingUserRedisRepository.getUserWaitingStoreIds(userId);
		// if (storeIds.isEmpty())
		// 	return Collections.emptyList();
		List<Long> storeIds = members.stream()
			.map(m -> Long.parseLong(m.substring(0, m.indexOf(':'))))
			.distinct()
			.toList();

		// 2) Store, Department 배치 조회
		List<Store> stores = storeRepository.findAllWithDepartmentByStoreIdIn(storeIds);
		Map<Long, Store> storeMap = stores.stream()
			.collect(Collectors.toMap(Store::getStoreId, Function.identity()));

		// 3) Department 이름 조회 (departmentId 기준)
		Set<Long> deptIds = stores.stream()
			.map(Store::getDepartmentId)
			.collect(Collectors.toSet());
		Map<Long, String> deptNameMap = departmentRepository.findAllById(deptIds).stream()
			.collect(Collectors.toMap(Department::getId, Department::getName));

		// 4) StoreImage 조회 (프로필 + 배너)
		List<ImageType> neededTypes = List.of(ImageType.PROFILE, ImageType.BANNER);
		List<StoreImage> images = storeImageRepository.findAllByStore_StoreIdInAndImageTypeIn(storeIds, neededTypes);

		Map<Long, String> profileMap = images.stream()
			.filter(img -> img.getImageType() == ImageType.PROFILE)
			.collect(Collectors.toMap(
				img -> img.getStore().getStoreId(),
				StoreImage::getImageUrl,
				(first, second) -> first
			));
		Map<Long, List<String>> bannerMap = images.stream()
			.filter(img -> img.getImageType() == ImageType.BANNER)
			.collect(Collectors.groupingBy(
				img -> img.getStore().getStoreId(),
				Collectors.mapping(StoreImage::getImageUrl, Collectors.toList())
			));

		List<Object> pipelineResults = redisTemplate.executePipelined((RedisCallback<Object>)conn -> {
			byte[] uid = redisTemplate.getStringSerializer().serialize(userId);
			for (Long storeId : storeIds) {
				String qk = RedisKeyUtils.buildWaitingKeyPrefix() + storeId;
				String pk = RedisKeyUtils.buildWaitingPartySizeKeyPrefix() + storeId;
				String sk = RedisKeyUtils.buildWaitingStatusKeyPrefix() + storeId;
				String nk = RedisKeyUtils.buildReservationNumberKey(storeId);
				conn.zRank(qk.getBytes(), uid);      // 순번 (0-based)
				conn.hGet(pk.getBytes(), uid);       // partySize (String)
				conn.zScore(qk.getBytes(), uid);     // timestamp (Double)
				conn.hGet(sk.getBytes(), uid);       // status (String)
				conn.hGet(nk.getBytes(), uid);       // reservationId (String)
			}
			return null;
		});

		// 4) 결과 매핑
		List<MyWaitingQueueDto> result = new ArrayList<>(storeIds.size());
		Iterator<Object> it = pipelineResults.iterator();
		for (Long storeId : storeIds) {
			Store store = storeMap.get(storeId);
			Long rankObj = (Long)it.next(); // null 가능
			String partyStr = (String)it.next();
			Double tsScore = (Double)it.next();
			String status = (String)it.next();
			String reservationId = (String)it.next();


			String activeReservationId = activeResIdByStore.get(storeId);
			// 유령 감지: 큐에 없음/번호 불일치/번호 null
			boolean zombie = (rankObj == null) || (reservationId == null) ||
							 (activeReservationId != null && !activeReservationId.equals(reservationId));
			if (zombie) {
				try {
					String toRemove = (activeReservationId != null) ? activeReservationId
						: (reservationId != null ? reservationId : null);
					if (toRemove != null) {
						waitingPermitLuaRepository.removeActiveMember(userId, String.valueOf(storeId), toRemove);
					}
				} catch (Exception ignore) {}
				continue; // 목록에서 제외
			}


			int rank = (rankObj != null ? rankObj.intValue() + 1 : 0);
			int teamsAhead = (rankObj != null ? rankObj.intValue() : 0);
			int partySize = (partyStr != null ? Integer.parseInt(partyStr) : 0);
			LocalDateTime registeredAt = tsScore != null
				? Instant.ofEpochMilli(tsScore.longValue())
				.atZone(ZoneId.of("Asia/Seoul"))
				.toLocalDateTime()
				: null;

			result.add(MyWaitingQueueDto.builder()
				.reservationId(reservationId)
				.storeId(storeId)
				.storeName(store.getName())
				.departmentName(deptNameMap.get(store.getDepartmentId()))
				.location(store.getLocation())
				.profileImageUrl(profileMap.getOrDefault(storeId, ""))
				.bannerImageUrl(bannerMap.getOrDefault(storeId, Collections.emptyList()))
				.rank(rank)
				.teamsAhead(teamsAhead)
				.partySize(partySize)
				.status(status)
				.registeredAt(registeredAt)
				.build()
			);
		}

		return result;
	}

	@Transactional
	public ReservationCreateResponseDto create(Long storeId, CustomOAuth2User customOAuth2User,
		ReservationCreateRequestDto requestDto) {
		Store store = storeRepository.findById(storeId)
			.orElseThrow(StoreNotFoundException::new);
		User user = userRepository.findById(customOAuth2User.getUserId())
			.orElseThrow(UserNotFoundException::new);
		// store 웨이팅 비활성화 여부
		if (Boolean.FALSE.equals(store.getIsActive()))
			throw new StoreWaitingDisabledException();
		// 중복 예약 존재 여부 확인
		boolean hasOngoingReservation = reservationRepository.existsByUserAndStoreAndStatusIn(
			user,
			store,
			List.of(ReservationStatus.WAITING, ReservationStatus.CALLING)
		);
		if (hasOngoingReservation) {
			throw new DuplicateReservationException();
		}
		Reservation reservation = Reservation.builder()
			.store(store)
			.user(user)
			.requestedAt(LocalDateTime.now())
			.status(ReservationStatus.WAITING)
			.partySize(requestDto.getPartySize())
			.build();

		Reservation saved = reservationRepository.save(reservation);

		return ReservationCreateResponseDto.builder()
			.id(saved.getId())
			.storeId(saved.getStore().getStoreId())
			.userId(saved.getUser().getId())
			.requestedAt(saved.getRequestedAt())
			.status(saved.getStatus().name())
			.partySize(saved.getPartySize())
			.build();
	}
}

