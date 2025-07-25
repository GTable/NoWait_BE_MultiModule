package com.nowait.applicationadmin.reservation.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationadmin.reservation.dto.CallGetResponseDto;
import com.nowait.applicationadmin.reservation.dto.CallingWaitingResponseDto;
import com.nowait.applicationadmin.reservation.dto.ReservationGetResponseDto;
import com.nowait.applicationadmin.reservation.dto.ReservationStatusSummaryDto;
import com.nowait.applicationadmin.reservation.dto.ReservationStatusUpdateRequestDto;
import com.nowait.applicationadmin.reservation.dto.WaitingUserResponse;
import com.nowait.applicationadmin.reservation.repository.WaitingRedisRepository;
import com.nowait.common.enums.ReservationStatus;
import com.nowait.common.enums.Role;
import com.nowait.domaincorerdb.reservation.entity.Reservation;
import com.nowait.domaincorerdb.reservation.exception.ReservationNotFoundException;
import com.nowait.domaincorerdb.reservation.exception.ReservationUpdateUnauthorizedException;
import com.nowait.domaincorerdb.reservation.exception.ReservationViewUnauthorizedException;
import com.nowait.domaincorerdb.reservation.repository.ReservationRepository;
import com.nowait.domaincorerdb.store.repository.StoreRepository;
import com.nowait.domaincorerdb.user.entity.MemberDetails;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domaincorerdb.user.exception.UserNotFoundException;
import com.nowait.domaincorerdb.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationService {

	private final ReservationRepository reservationRepository;
	private final UserRepository userRepository;
	private final WaitingRedisRepository waitingRedisRepository;
	private final StoreRepository storeRepository;
	//TODO 성능 비교를 위해 남겨둔 로직
	@Transactional(readOnly = true)
	public ReservationStatusSummaryDto getReservationListByStoreId(Long storeId, MemberDetails memberDetails) {
		User user =  userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
		if (!Role.SUPER_ADMIN.equals(user.getRole()) && !user.getStoreId().equals(storeId)) {
			throw new ReservationViewUnauthorizedException();
		}
		List<Reservation> reservations = reservationRepository.findAllByStore_StoreIdOrderByRequestedAtAsc(storeId);

		// 상태별 카운트 집계
		int waitingCount = 0;
		int confirmedCount = 0;
		int cancelledCount = 0;
		int callingCount = 0;
		List<ReservationGetResponseDto> reservationDtoList = new ArrayList<>();
		for (Reservation r : reservations) {
			if (r.getStatus() == ReservationStatus.WAITING) waitingCount++;
			if (r.getStatus() == ReservationStatus.CONFIRMED) confirmedCount++;
			if (r.getStatus() == ReservationStatus.CANCELLED) cancelledCount++;
			if (r.getStatus() == ReservationStatus.CALLING) callingCount++;
			reservationDtoList.add(ReservationGetResponseDto.fromEntity(r));
		}

		return ReservationStatusSummaryDto.builder()
			.waitingCount(waitingCount)
			.confirmedCount(confirmedCount)
			.cancelledCount(cancelledCount)
			.callingCount(callingCount)
			.reservationList(reservationDtoList)
			.build();
	}
	//TODO 성능 비교를 위해 남겨둔 로직
	@Transactional
	public CallGetResponseDto updateReservationStatus(Long reservationId, ReservationStatusUpdateRequestDto requestDto,
		MemberDetails memberDetails) {
		User user =  userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
		Reservation reservation = reservationRepository.findById(reservationId).orElseThrow(ReservationNotFoundException::new);
		if (!Role.SUPER_ADMIN.equals(user.getRole()) && !user.getStoreId().equals(reservation.getStore().getStoreId())) {
			throw new ReservationUpdateUnauthorizedException();
		}
			reservation.updateStatus(requestDto.getStatus());
		return CallGetResponseDto.fromEntity(reservation);
	}
	// Redis queue에 있는 주점별 전체 대기열 조회
	@Transactional(readOnly = true)
	public List<WaitingUserResponse> getAllWaitingUserDetails(Long storeId) {
		List<ZSetOperations.TypedTuple<String>> waitingList = waitingRedisRepository.getAllWaitingWithScore(storeId);

		return waitingList.stream()
			.map(tuple -> {
				String userId = tuple.getValue();

				// 1. Redis에서 partySize/status 조회
				Integer partySize = waitingRedisRepository.getWaitingPartySize(storeId, userId);
				String status = waitingRedisRepository.getWaitingStatus(storeId, userId);

				// 2. DB에서 userName, createdAt, reservationId 조회
				String userName = null;
				LocalDateTime createdAt = null;
				Long reservationId = null;

				Optional<Reservation> reservationOpt = reservationRepository.findByStore_StoreIdAndUserId(storeId, Long.valueOf(userId));
				if (reservationOpt.isPresent()) {
					Reservation reservation = reservationOpt.get();
					createdAt = reservation.getRequestedAt();
					reservationId = reservation.getId();
					userName = reservation.getUser().getNickname();
				}

				return new WaitingUserResponse(
					reservationId != null ? reservationId.toString() : null,
					userId,
					partySize,
					userName,
					createdAt,
					status,
					tuple.getScore()
				);
			})
			.toList();

	}

	// 완료 or 취소 처리된 대기 리스트 조회
	@Transactional(readOnly = true)
	public List<WaitingUserResponse> getCompletedWaitingUserDetails(Long storeId) {
		List<Reservation> reservations = reservationRepository.findAllByStore_StoreId(storeId);

		return reservations.stream()
			.map(r -> WaitingUserResponse.fromEntity(r))
			.toList();
	}

	// 대기 객체 호출 (WAITING -> CALLING)
	@Transactional
	public CallingWaitingResponseDto callWaiting(Long storeId, String userId, MemberDetails memberDetails) {
		User user =  userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
		if (!Role.SUPER_ADMIN.equals(user.getRole()) && !user.getStoreId().equals(storeId)) {
			throw new ReservationViewUnauthorizedException();
		}
		String status = waitingRedisRepository.getWaitingStatus(storeId, userId);
		if (!"WAITING".equals(status)) {
			throw new IllegalStateException("이미 호출되었거나 없는 예약입니다.");
		}
		Integer partySize = waitingRedisRepository.getWaitingPartySize(storeId, userId);
		waitingRedisRepository.setWaitingStatus(storeId, userId, "CALLING");
		// [2] DB에 상태 영구 저장 (없으면 생성, 있으면 상태만 변경)
		Reservation reservation = reservationRepository.findByStore_StoreIdAndUserId(storeId, Long.valueOf(userId))
			.orElse(
				Reservation.builder()
					.store(storeRepository.getReferenceById(storeId))
					.user(user)
					.requestedAt(LocalDateTime.now())
					.partySize(partySize)
					.build()
			);
		reservation.updateStatus(ReservationStatus.CALLING); // setter 대신 빌더로 새 객체 or withStatus 패턴 추천
		reservationRepository.save(reservation);
		return CallingWaitingResponseDto.builder()
			.storeId(storeId)
			.userId(userId)
			.status(reservation.getStatus().name())
			.calledAt(reservation.getRequestedAt())
			.build();
	}
	// 대기 객체 상태 변경
	@Transactional
	public String processEntryStatus(Long storeId, String userId, MemberDetails memberDetails, ReservationStatus status) {
		// (권한 체크 필요시 여기에 추가)
		User user = userRepository.findById(memberDetails.getId())
			.orElseThrow(UserNotFoundException::new);
		if (!Role.SUPER_ADMIN.equals(user.getRole()) && !user.getStoreId().equals(storeId)) {
			throw new ReservationViewUnauthorizedException();
		}
		// 1. DB status 업데이트
		Reservation reservation = reservationRepository.findByStore_StoreIdAndUserId(storeId, Long.valueOf(userId))
			.orElseThrow(() -> new IllegalArgumentException("해당 예약이 존재하지 않습니다."));
		reservation.updateStatus(status);
		// 2. Redis에서 삭제
		waitingRedisRepository.deleteWaiting(storeId, userId);

		// 메시지 동적 반환
		String action = (status == ReservationStatus.CONFIRMED) ? "입장 완료" : "입장 취소";
		return user.getNickname() + "님의 예약이 " + action + " 처리되었습니다.";
	}


}

