package com.nowait.applicationuser.reservation.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationuser.reservation.dto.MyWaitingQueueDto;
import com.nowait.applicationuser.reservation.dto.ReservationCreateRequestDto;
import com.nowait.applicationuser.reservation.dto.ReservationCreateResponseDto;
import com.nowait.applicationuser.reservation.dto.WaitingResponseDto;
import com.nowait.applicationuser.reservation.repository.WaitingUserRedisRepository;
import com.nowait.common.enums.ReservationStatus;
import com.nowait.common.enums.Role;
import com.nowait.domaincorerdb.department.entity.Department;
import com.nowait.domaincorerdb.department.repository.DepartmentRepository;
import com.nowait.domaincorerdb.reservation.entity.Reservation;
import com.nowait.domaincorerdb.reservation.exception.DuplicateReservationException;
import com.nowait.domaincorerdb.reservation.repository.ReservationRepository;
import com.nowait.domaincorerdb.store.entity.Store;
import com.nowait.domaincorerdb.store.exception.StoreNotFoundException;
import com.nowait.domaincorerdb.store.exception.StoreWaitingDisabledException;
import com.nowait.domaincorerdb.store.repository.StoreRepository;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domaincorerdb.user.exception.UserNotFoundException;
import com.nowait.domaincorerdb.user.repository.UserRepository;
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

	public WaitingResponseDto registerWaiting(
		Long storeId,CustomOAuth2User customOAuth2User,ReservationCreateRequestDto requestDto
	) {
		// Store 유효성 검증 추가
		Store store = storeRepository.findById(storeId)
			.orElseThrow(StoreNotFoundException::new);
		if (Boolean.FALSE.equals(store.getIsActive()))
			throw new StoreWaitingDisabledException();
		// User Role 검증 추가
		User user = userRepository.findById(customOAuth2User.getUserId())
			.orElseThrow(UserNotFoundException::new);
		if (user.getRole() == Role.MANAGER) {
			throw new IllegalArgumentException("Manager cannot register waiting");
		}

		String userId = customOAuth2User.getUserId().toString();
		long timestamp = System.currentTimeMillis();

		// 예약 신청 유저 큐(queue)에 추가
		boolean added = waitingUserRedisRepository.addToWaitingQueue(storeId, userId, requestDto.getPartySize(), timestamp);
		if (!added) {
			throw new IllegalArgumentException("Failed to add to waiting queue");
		}
		// 신규 등록/기존 등록 관계없이 내 순번, 전체 인원 반환
		Long rank = waitingUserRedisRepository.getRank(storeId, userId);
		return WaitingResponseDto.builder()
			.rank(rank == null ? -1 : rank.intValue() + 1)
			.partySize(requestDto.getPartySize() == null ? 0 : requestDto.getPartySize())
			.build();
	}

	public WaitingResponseDto myWaitingInfo(Long storeId, CustomOAuth2User customOAuth2User) {
		String userId = customOAuth2User.getUserId().toString();
		// 입력 검증 추가
		if (storeId == null || userId.trim().isEmpty()) {
		throw new IllegalArgumentException("Invalid storeId or userId");
		}
		Long rank = waitingUserRedisRepository.getRank(storeId, userId);
		Integer partySize = waitingUserRedisRepository.getPartySize(storeId, userId);
		return WaitingResponseDto.builder()
			.rank(rank == null ? -1 : rank.intValue() + 1)
			.partySize(partySize == null ? 0 : partySize)
			.build();
	}

	public boolean cancelWaiting(Long storeId, CustomOAuth2User customOAuth2User) {
		String userId = customOAuth2User.getUserId().toString();
		if (storeId == null || userId.trim().isEmpty()) {
			throw new IllegalArgumentException("Invalid storeId or userId");
		}
		// 대기열에서 제거 및 결과 반환
		boolean removed = waitingUserRedisRepository.removeWaiting(storeId, userId);
		if (!removed) {
			throw new IllegalArgumentException("Waiting not found");
		}
		return removed;
	}
	//TODO 성능 개선 필요
	public List<MyWaitingQueueDto> getAllMyWaitings(CustomOAuth2User customOAuth2User) {
		String userId = customOAuth2User.getUserId().toString();
		List<Long> userWaitingStoreIds = waitingUserRedisRepository.getUserWaitingStoreIds(userId);

		List<MyWaitingQueueDto> result = new ArrayList<>();
		if (!userWaitingStoreIds.isEmpty()) {
			// Store, Department 배치 조회
			List<Store> stores = storeRepository.findAllById(userWaitingStoreIds);
			Map<Long, Store> storeMap = stores.stream()
				.collect(Collectors.toMap(Store::getStoreId, Function.identity()));

			Set<Long> departmentIds = stores.stream()
				.map(Store::getDepartmentId)
				.collect(Collectors.toSet());
			Map<Long, String> departmentNameMap = departmentRepository.findAllById(departmentIds).stream()
				.collect(Collectors.toMap(Department::getId, Department::getName));

			for (Long storeId : userWaitingStoreIds) {
				Store store = storeMap.get(storeId);
				if (store == null) continue;

				Long rank = waitingUserRedisRepository.getRank(storeId, userId);
				Integer partySize = waitingUserRedisRepository.getPartySize(storeId, userId);
				Long timestamp = waitingUserRedisRepository.getWaitingTimestamp(storeId, userId);

				LocalDateTime registeredAt = timestamp != null
					? LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.of("Asia/Seoul"))
					: null;

				result.add(MyWaitingQueueDto.builder()
					.storeId(storeId)
					.storeName(store.getName())
					.departmentName(departmentNameMap.get(store.getDepartmentId()))
					.rank(rank != null ? rank.intValue() + 1 : 0)
					.teamsAhead(rank != null ? rank.intValue() : 0)
					.partySize(partySize != null ? partySize : 0)
					.status(waitingUserRedisRepository.getWaitingStatus(storeId, userId)) // 필요시 redis에 상태값이 있으면 조회해서 세팅
					.registeredAt(registeredAt)
					.location(store.getLocation())
					.profileImageUrl(customOAuth2User.getUser().getProfileImage())
					.build());
			}
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

