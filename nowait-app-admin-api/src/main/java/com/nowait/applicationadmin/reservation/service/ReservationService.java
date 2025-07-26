package com.nowait.applicationadmin.reservation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationadmin.reservation.dto.CallGetResponseDto;
import com.nowait.applicationadmin.reservation.dto.CallingWaitingResponseDto;
import com.nowait.applicationadmin.reservation.dto.EntryStatusResponseDto;
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
import com.nowait.domaincorerdb.store.entity.Store;
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
		User user = userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
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
			if (r.getStatus() == ReservationStatus.WAITING)
				waitingCount++;
			if (r.getStatus() == ReservationStatus.CONFIRMED)
				confirmedCount++;
			if (r.getStatus() == ReservationStatus.CANCELLED)
				cancelledCount++;
			if (r.getStatus() == ReservationStatus.CALLING)
				callingCount++;
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
		User user = userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
		Reservation reservation = reservationRepository.findById(reservationId)
			.orElseThrow(ReservationNotFoundException::new);
		if (!Role.SUPER_ADMIN.equals(user.getRole()) && !user.getStoreId()
			.equals(reservation.getStore().getStoreId())) {
			throw new ReservationUpdateUnauthorizedException();
		}
		reservation.updateStatus(requestDto.getStatus());
		return CallGetResponseDto.fromEntity(reservation);
	}

	// Redis queue에 있는 주점별 전체 대기열 조회
	@Transactional(readOnly = true)
	public List<WaitingUserResponse> getAllWaitingUserDetails(Long storeId) {
		List<ZSetOperations.TypedTuple<String>> waitingList = waitingRedisRepository.getAllWaitingWithScore(storeId);
		System.out.println(waitingList);

		// TODO N + 1 발생 -> 개선 필요
		return waitingList.stream()
			.map(tuple -> {
				String userId = tuple.getValue();

				// 1. Redis에서 partySize/status 조회
				Integer partySize = waitingRedisRepository.getWaitingPartySize(storeId, userId);
				String status = waitingRedisRepository.getWaitingStatus(storeId, userId);

				// 2. DB에서 userName, createdAt, reservationId 조회
				//TODO 개선필요 -> createAt 정확성 및 reservationhId 생성 방법(예약생성부터 DB에 박아야하나....)
				String userName = userRepository.getReferenceById(Long.valueOf(userId)).getNickname();
				LocalDateTime createdAt = LocalDateTime.now();
				String reservationId = String.valueOf(
					ThreadLocalRandom.current().nextInt(1, 100));

				Optional<Reservation> reservationOpt = reservationRepository.findFirstByStore_StoreIdAndUserIdAndRequestedAtBetweenOrderByRequestedAtDesc(
					storeId, Long.valueOf(userId), LocalDate.now().atStartOfDay(),
					LocalDate.now().atTime(LocalTime.MAX));
				if (reservationOpt.isPresent()) {
					Reservation reservation = reservationOpt.get();
					createdAt = reservation.getRequestedAt();
					reservationId = reservation.getId().toString();
					userName = reservation.getUser().getNickname();
				} else {
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
		List<Reservation> reservations = reservationRepository.findAllByStore_StoreIdAndStatusInAndRequestedAtBetween(
			storeId,
			List.of(ReservationStatus.CONFIRMED, ReservationStatus.CANCELLED),
			LocalDate.now().atStartOfDay(),
			LocalDate.now().atTime(LocalTime.MAX));

		return reservations.stream()
			.map(r -> WaitingUserResponse.fromEntity(r))
			.toList();
	}

	private User authorize(Long storeId, MemberDetails member) {
		User u = userRepository.findById(member.getId())
			.orElseThrow(UserNotFoundException::new);
		if (!Role.SUPER_ADMIN.equals(u.getRole()) && !storeId.equals(u.getStoreId())) {
			throw new ReservationViewUnauthorizedException();
		}
		return u;
	}

	// 공통: 오늘 날짜 예약 조회
	private Reservation findTodayReservation(Long storeId, String userId) {
		LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
		LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

		return reservationRepository
			.findByStore_StoreIdAndUserIdAndStatusInAndRequestedAtBetween(
				storeId,
				Long.valueOf(userId),
				List.of(ReservationStatus.WAITING, ReservationStatus.CALLING),
				startOfDay,
				endOfDay
			)
			.orElseThrow(() -> new IllegalArgumentException("오늘 날짜의 예약이 존재하지 않습니다."));
	}

	/**
	 * 상태를 하나의 메서드에서 처리합니다.
	 * - CALLING   : Redis 상태를 CALLING으로 변경 → DB 저장 → calledAt 반환
	 * - CONFIRMED : Redis에서 삭제             → DB 저장 → 완료 메시지 반환
	 * - CANCELLED : Redis에서 삭제             → DB 저장 → 취소 메시지 반환
	 */
	@Transactional
	public EntryStatusResponseDto processEntryStatus(
		Long storeId,
		String userId,
		MemberDetails member,
		ReservationStatus newStatus
	) {
		User manager = authorize(storeId, member);
		User user = userRepository.findById(Long.valueOf(userId)).orElseThrow(UserNotFoundException::new);

		String message = null;
		Reservation reservation;

		switch (newStatus) {
			case CALLING:
				// 1) Redis 상태 검사 & 변경
				String curr = waitingRedisRepository.getWaitingStatus(storeId, userId);
				if (!ReservationStatus.WAITING.name().equals(curr)) {
					throw new IllegalStateException("이미 호출되었거나 없는 예약입니다.");
				}
				waitingRedisRepository.setWaitingStatus(storeId, userId, ReservationStatus.CALLING.name());

				// 2) 파티 인원, 호출 시각
				Integer partySize = waitingRedisRepository.getWaitingPartySize(storeId, userId);
				LocalDateTime now = LocalDateTime.now();

				// 3) DB에 무조건 새로 저장
				Store store = storeRepository.getReferenceById(storeId);
				reservation = Reservation.builder()
					.store(store)
					.user(user)
					.partySize(partySize)
					.requestedAt(now)
					.status(ReservationStatus.CALLING)
					.build();
				reservationRepository.save(reservation);

				break;

			case CONFIRMED:
			case CANCELLED:
				// 1) Redis에서 제거
				waitingRedisRepository.deleteWaiting(storeId, userId);

				// 2) 오늘 날짜 예약 조회 & 상태 변경
				reservation = findTodayReservation(storeId, userId);
				reservation.updateStatus(newStatus);
				reservationRepository.save(reservation);

				// 3) 완료/취소 메시지
				message = String.format(
					"%s님의 예약이 %s 처리되었습니다.",
					user.getNickname(),
					newStatus == ReservationStatus.CONFIRMED ? "입장 완료" : "입장 취소"
				);
				break;

			default:
				throw new IllegalArgumentException("지원하지 않는 상태입니다: " + newStatus);
		}

		// 5) 공통 DTO 반환
		return EntryStatusResponseDto.builder()
			.id(reservation.getId().toString())
			.userId(userId)
			.partySize(reservation.getPartySize())
			.userName(user.getNickname())
			.createdAt(reservation.getRequestedAt())
			.status(reservation.getStatus().name())
			.message(message)
			.build();
	}

}

