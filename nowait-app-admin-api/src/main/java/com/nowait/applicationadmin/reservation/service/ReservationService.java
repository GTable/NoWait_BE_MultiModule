package com.nowait.applicationadmin.reservation.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationadmin.reservation.dto.CallGetResponseDto;
import com.nowait.applicationadmin.reservation.dto.CallingWaitingResponseDto;
import com.nowait.applicationadmin.reservation.dto.ReservationGetResponseDto;
import com.nowait.applicationadmin.reservation.dto.ReservationStatusSummaryDto;
import com.nowait.applicationadmin.reservation.dto.ReservationStatusUpdateRequestDto;
import com.nowait.applicationadmin.reservation.repository.WaitingRedisRepository;
import com.nowait.common.enums.ReservationStatus;
import com.nowait.common.enums.Role;
import com.nowait.domaincorerdb.order.exception.OrderUpdateUnauthorizedException;
import com.nowait.domaincorerdb.reservation.entity.Reservation;
import com.nowait.domaincorerdb.reservation.exception.ReservationNotFoundException;
import com.nowait.domaincorerdb.reservation.exception.ReservationUpdateUnauthorizedException;
import com.nowait.domaincorerdb.reservation.exception.ReservationViewUnauthorizedException;
import com.nowait.domaincorerdb.reservation.repository.ReservationRepository;
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
	public CallingWaitingResponseDto callWaiting(Long storeId, String userId, MemberDetails memberDetails) {
		User user =  userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
		if (!Role.SUPER_ADMIN.equals(user.getRole()) && !user.getStoreId().equals(storeId)) {
			throw new ReservationViewUnauthorizedException();
		}
		String status = waitingRedisRepository.getWaitingStatus(storeId, userId);
		if (!"WAITING".equals(status)) {
			throw new IllegalStateException("이미 호출되었거나 없는 예약입니다.");
		}
		waitingRedisRepository.setWaitingStatus(storeId, userId, "CALLING");
		LocalDateTime calledAt = LocalDateTime.now();
		return CallingWaitingResponseDto.builder()
			.storeId(storeId)
			.userId(userId)
			.status("CALLING")
			.calledAt(calledAt)
			.build();
	}
	//TODO CALLING -> 입장완료 처리 로직 구현 필요(redis에서 삭제 후 RDB에 저장하는게 나을지??)

}

