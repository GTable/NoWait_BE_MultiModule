package com.example.apiuser.reservation.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.apiuser.reservation.dto.ReservationCreateRequestDto;
import com.example.apiuser.reservation.dto.ReservationCreateResponseDto;
import com.example.domainstore.entity.Store;
import com.example.domainstore.repository.StoreRepository;
import com.nowait.auth.dto.CustomOAuth2User;
import com.nowait.domainreservation.entity.Reservation;
import com.nowait.domainreservation.repository.ReservationRepository;
import com.nowaiting.common.enums.ReservationStatus;
import com.nowaiting.user.entity.User;
import com.nowaiting.user.exception.UserNotFoundException;
import com.nowaiting.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationService {

	private final ReservationRepository reservationRepository;
	private final StoreRepository storeRepository;
	private final UserRepository userRepository;

	@Transactional
	public ReservationCreateResponseDto create(Long storeId, CustomOAuth2User customOAuth2User,
		ReservationCreateRequestDto requestDto) {
		Store store = storeRepository.findById(storeId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 store"));
		User user = userRepository.findById(customOAuth2User.getUserId())
			.orElseThrow(UserNotFoundException::new);

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

