package com.nowait.applicationadmin.reservation.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationadmin.reservation.dto.CallGetResponseDto;
import com.nowait.applicationadmin.reservation.dto.ReservationGetResponseDto;
import com.nowait.applicationadmin.reservation.dto.ReservationStatusSummaryDto;
import com.nowait.applicationadmin.reservation.dto.ReservationStatusUpdateRequestDto;
import com.nowait.reservation.entity.Reservation;
import com.nowait.reservation.repository.ReservationRepository;
import com.nowait.common.enums.ReservationStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationService {

	private final ReservationRepository reservationRepository;

	@Transactional(readOnly = true)
	public ReservationStatusSummaryDto getReservationListByStoreId(Long storeId) {
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
	public CallGetResponseDto updateReservationStatus(Long reservationId, ReservationStatusUpdateRequestDto requestDto) {
		Reservation reservation = reservationRepository.findById(reservationId)
			.orElseThrow(ReservationNotFoundException::new);
			reservation.updateStatus(requestDto.getStatus());
		return CallGetResponseDto.fromEntity(reservation);
	}


}

