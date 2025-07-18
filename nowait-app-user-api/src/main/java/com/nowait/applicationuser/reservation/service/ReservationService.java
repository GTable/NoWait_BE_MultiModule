package com.nowait.applicationuser.reservation.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationuser.reservation.dto.ReservationCreateRequestDto;
import com.nowait.applicationuser.reservation.dto.ReservationCreateResponseDto;
import com.nowait.applicationuser.reservation.dto.WaitingResponseDto;
import com.nowait.applicationuser.reservation.repository.WaitingRedisRepository;
import com.nowait.common.enums.ReservationStatus;
import com.nowait.common.enums.Role;
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
	private final WaitingRedisRepository waitingRedisRepository;

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
		boolean added = waitingRedisRepository.addToWaitingQueue(storeId, userId, requestDto.getPartySize(), timestamp);
		if (!added) {
			throw new IllegalArgumentException("Failed to add to waiting queue");
		}
		// 신규 등록/기존 등록 관계없이 내 순번, 전체 인원 반환
		Long rank = waitingRedisRepository.getRank(storeId, userId);
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
		Long rank = waitingRedisRepository.getRank(storeId, userId);
		Integer partySize = waitingRedisRepository.getPartySize(storeId, userId);
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
		boolean removed = waitingRedisRepository.removeWaiting(storeId, userId);
		if (!removed) {
			throw new IllegalArgumentException("Waiting not found");
		}
		return removed;
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

