package com.nowait.applicationuser.waiting.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.nowait.applicationuser.waiting.dto.RegisterWaitingRequest;
import com.nowait.applicationuser.waiting.dto.RegisterWaitingResponse;
import com.nowait.applicationuser.waiting.event.AddWaitingRegisterEvent;
import com.nowait.domaincorerdb.reservation.entity.Reservation;
import com.nowait.domaincorerdb.reservation.repository.ReservationRepository;
import com.nowait.domaincorerdb.store.entity.Store;
import com.nowait.domaincorerdb.store.exception.StoreNotFoundException;
import com.nowait.domaincorerdb.store.repository.StoreRepository;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domaincorerdb.user.exception.UserNotFoundException;
import com.nowait.domaincorerdb.user.repository.UserRepository;
import com.nowait.domaincoreredis.reservation.repository.WaitingRedisRepository;
import com.nowait.domainuserrdb.oauth.dto.CustomOAuth2User;

@ExtendWith(MockitoExtension.class)
class WaitingServiceTest {

	@InjectMocks
	private WaitingService waitingService;
	@Mock
	private ApplicationEventPublisher eventPublisher;
	@Mock
	private StoreRepository storeRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private WaitingRedisRepository waitingRedisRepository;
	@Mock
	private ReservationRepository reservationRepository;

	@Test
	@DisplayName("웨이팅 정상 등록 테스트")
	void registerWaiting() {
		// given
		CustomOAuth2User customOAuth2User = mock(CustomOAuth2User.class);
		RegisterWaitingRequest request = new RegisterWaitingRequest(4);

		Long storeId = 1L;
		Long userId = 1L;

		Store store = Store.builder().storeId(storeId).build();
		User user = User.builder().id(userId).build();


		// when
		when(storeRepository.findById(storeId)).thenReturn(java.util.Optional.of(store));
		when(customOAuth2User.getUserId()).thenReturn(userId);
		when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
		RegisterWaitingResponse response = waitingService.registerWaiting(customOAuth2User, storeId, request);

		// then
		verify(waitingRedisRepository).indempotencyKeyExists(anyString(), eq("WAITING"));
		verify(reservationRepository).save(any(Reservation.class));
		verify(eventPublisher).publishEvent(any(AddWaitingRegisterEvent.class));

		assertThat(response.getPartySize()).isEqualTo(4);
		assertThat(response.getWaitingNumber()).isNotNull();
	}

	@Test
	@DisplayName("DB 저장 실패 시 Redis 대기열 추가 이벤트 실행 되지 않음")
	void registerWaiting_dbSaveFail() {
		// given
		CustomOAuth2User customOAuth2User = mock(CustomOAuth2User.class);
		RegisterWaitingRequest request = new RegisterWaitingRequest(4);

		Long storeId = 1L;
		Long userId = 1L;

		Store store = Store.builder().storeId(storeId).build();
		User user = User.builder().id(userId).build();

		// when
		when(customOAuth2User.getUserId()).thenReturn(userId);
		when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));

		when(reservationRepository.save(any(Reservation.class))).thenThrow(new RuntimeException("DB 저장 실패"));

		// then
		assertThatThrownBy(() ->
			waitingService.registerWaiting(customOAuth2User, storeId, request)
		).isInstanceOf(RuntimeException.class);

		verify(waitingRedisRepository).indempotencyKeyExists(anyString(), eq("WAITING"));
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	@DisplayName("없는 주점에 웨이팅 등록 시도 시 예외 발생")
	void registerWaiting_storeNotFound() {
		// given
		CustomOAuth2User customOAuth2User = mock(CustomOAuth2User.class);
		RegisterWaitingRequest request = new RegisterWaitingRequest(4);

		Long storeId = -1L; // 존재하지 않는 주점 ID
		Long userId = 1L;

		// when
		when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

		// then
		assertThatThrownBy(() ->
			waitingService.registerWaiting(customOAuth2User, storeId, request)
		).isInstanceOf(StoreNotFoundException.class);

		verify(userRepository, never()).findById(anyLong());
		verify(waitingRedisRepository, never()).indempotencyKeyExists(anyString(), eq("WAITING"));
		verify(reservationRepository, never()).save(any(Reservation.class));
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	@DisplayName("없는 유저가 웨이팅 등록 시도 시 예외 발생")
	void registerWaiting_userNotFound() {
		// given
		CustomOAuth2User customOAuth2User = mock(CustomOAuth2User.class);
		RegisterWaitingRequest request = new RegisterWaitingRequest(4);

		Long storeId = 1L;
		Long userId = -1L; // 존재 하지 않는 유저 ID

		Store store = Store.builder().storeId(storeId).build();

		// when
		when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
		when(customOAuth2User.getUserId()).thenReturn(userId);
		when(userRepository.findById(userId)).thenReturn(Optional.empty());

		// then
		assertThatThrownBy(() ->
			waitingService.registerWaiting(customOAuth2User, storeId, request)
		).isInstanceOf(UserNotFoundException.class);

		verify(waitingRedisRepository, never()).indempotencyKeyExists(anyString(), eq("WAITING"));
		verify(reservationRepository, never()).save(any(Reservation.class));
		verify(eventPublisher, never()).publishEvent(any());
	}
}
