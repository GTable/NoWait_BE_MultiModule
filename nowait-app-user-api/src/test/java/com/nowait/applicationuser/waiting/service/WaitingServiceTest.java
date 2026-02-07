package com.nowait.applicationuser.waiting.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.nowait.applicationuser.waiting.dto.RegisterWaitingRequest;
import com.nowait.applicationuser.waiting.dto.RegisterWaitingResponse;
import com.nowait.applicationuser.waiting.dto.WaitingIdempotencyValue;
import com.nowait.applicationuser.waiting.event.AddWaitingRegisterEvent;
import com.nowait.applicationuser.waiting.redis.WaitingIdempotencyRepository;
import com.nowait.domaincorerdb.reservation.entity.Reservation;
import com.nowait.domaincorerdb.reservation.repository.ReservationRepository;
import com.nowait.domaincorerdb.store.entity.Store;
import com.nowait.domaincorerdb.store.exception.StoreNotFoundException;
import com.nowait.domaincorerdb.store.repository.StoreRepository;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domaincorerdb.user.exception.UserNotFoundException;
import com.nowait.domaincorerdb.user.repository.UserRepository;
import com.nowait.domaincoreredis.reservation.exception.UserWaitingLimitExceededException;
import com.nowait.domaincoreredis.reservation.repository.WaitingRedisRepository;
import com.nowait.domainuserrdb.oauth.dto.CustomOAuth2User;

import jakarta.servlet.http.HttpServletRequest;

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
	@Mock
	private WaitingIdempotencyRepository waitingIdempotencyRepository;
	@Mock
	private HttpServletRequest httpServletRequest;
	@Mock
	private CustomOAuth2User customOAuth2User;

	private static final String IDEMPOTENCY_KEY = "550e8400-e29b-41d4-a716-446655440000";

	@BeforeEach
	void setUp() {
		when(httpServletRequest.getHeader("Idempotency-Key")).thenReturn(IDEMPOTENCY_KEY);
	}

	@Test
	@DisplayName("멱등키 있는 경우 미리 저장된 응답이 반환 되는지 테스트")
	void registerWaiting_idempotentKeyExists() {
		// given
		RegisterWaitingRequest request = new RegisterWaitingRequest(4);

		RegisterWaitingResponse idempotentResponse = RegisterWaitingResponse.builder()
			.waitingNumber("20260201-2-0001")
			.partySize(4)
			.build();

		when(waitingIdempotencyRepository.findByKey(IDEMPOTENCY_KEY))
			.thenReturn(Optional.of(new WaitingIdempotencyValue(
				"COMPLETED",
				idempotentResponse
			)));

		// when
		RegisterWaitingResponse response = waitingService.registerWaiting(
			customOAuth2User,
			"ZiVXAD1vVr5b",
			request,
			httpServletRequest
		);

		// then
		assertThat(response).isSameAs(idempotentResponse);

		verify(storeRepository, never()).findByPublicCodeAndDeletedFalse(anyString());
		verify(userRepository, never()).findById(anyLong());
		verify(waitingRedisRepository, never()).incrementAndCheckWaitingLimit(anyLong(), anyLong());
		verify(reservationRepository, never()).save(any(Reservation.class));
		verify(eventPublisher, never()).publishEvent(any());
		verify(waitingIdempotencyRepository, never()).saveIdempotencyValue(anyString(), any(RegisterWaitingResponse.class));
	}

	@Test
	@DisplayName("웨이팅 정상 등록 시 DB 저장, 이벤트 발생, 멱등 응답 저장 수행")
	void registerWaiting_success() {
		// given
		CustomOAuth2User customOAuth2User = mock(CustomOAuth2User.class);
		RegisterWaitingRequest request = new RegisterWaitingRequest(4);

		when(httpServletRequest.getHeader("Idempotency-Key")).thenReturn(IDEMPOTENCY_KEY);
		when(waitingIdempotencyRepository.findByKey(IDEMPOTENCY_KEY))
			.thenReturn(Optional.empty());

		Long userId = 10L;
		String publicCode = "ZiVXAD1vVr5b";

		Store store = Store.builder().publicCode(publicCode).build();
		User user = User.builder().id(userId).build();

		when(storeRepository.findByPublicCodeAndDeletedFalse(publicCode)).thenReturn(java.util.Optional.of(store));
		when(customOAuth2User.getUserId()).thenReturn(10L);
		when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

		doNothing()
			.when(waitingRedisRepository)
			.incrementAndCheckWaitingLimit(userId, 3L);

		when(waitingRedisRepository.incrementDailySequence(anyString())).thenReturn(1L);

		// when
		RegisterWaitingResponse response = waitingService.registerWaiting(
			customOAuth2User,
			publicCode,
			request,
			httpServletRequest
		);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getPartySize()).isEqualTo(4);
		assertThat(response.getWaitingNumber()).isNotBlank();

		verify(waitingRedisRepository).incrementAndCheckWaitingLimit(userId, 3L);
		verify(reservationRepository).save(any(Reservation.class));
		verify(eventPublisher).publishEvent(any(AddWaitingRegisterEvent.class));
		verify(waitingIdempotencyRepository).saveIdempotencyValue(anyString(), any(RegisterWaitingResponse.class));
	}

	@Test
	@DisplayName("DB 저장 중 예외 발생 시 이벤트 발행 및 멱등 저장이 수행되지 않음")
	void registerWaiting_dbSaveException() {
		// given
		when(waitingIdempotencyRepository.findByKey(IDEMPOTENCY_KEY))
			.thenReturn(Optional.empty());
		RegisterWaitingRequest request = new RegisterWaitingRequest(4);

		Store store = Store.builder().publicCode("ZiVXAD1vVr5b").build();
		User user = User.builder().id(10L).build();

		when(storeRepository.findByPublicCodeAndDeletedFalse("ZiVXAD1vVr5b"))
			.thenReturn(Optional.of(store));
		when(customOAuth2User.getUserId()).thenReturn(10L);
		when(userRepository.findById(10L))
			.thenReturn(Optional.of(user));

		doNothing().when(waitingRedisRepository).incrementAndCheckWaitingLimit(10L, 3L);

		when(waitingRedisRepository.incrementDailySequence(anyString())).thenReturn(1L);

		doThrow(new RuntimeException("DB 저장 실패"))
			.when(reservationRepository)
			.save(any(Reservation.class));


		// when & then
		assertThatThrownBy(() -> waitingService.registerWaiting(
			customOAuth2User,
			"ZiVXAD1vVr5b",
			request,
			httpServletRequest
		)).isInstanceOf(RuntimeException.class);

		verify(eventPublisher, never()).publishEvent(any(AddWaitingRegisterEvent.class));
		verify(waitingIdempotencyRepository, never()).saveIdempotencyValue(anyString(), any(RegisterWaitingResponse.class));
	}

	@Test
	@DisplayName("웨이팅 개수 제한 초과 시 예외 발생 테스트")
	void registerWaiting_exceedWaitingLimit() {
		// given
		CustomOAuth2User customOAuth2User = mock(CustomOAuth2User.class);
		RegisterWaitingRequest request = new RegisterWaitingRequest(4);

		when(httpServletRequest.getHeader("Idempotency-Key")).thenReturn(IDEMPOTENCY_KEY);
		when(waitingIdempotencyRepository.findByKey(IDEMPOTENCY_KEY))
			.thenReturn(Optional.empty());


		Long userId = 10L;
		String publicCode = "ZiVXAD1vVr5b";

		Store store = Store.builder().publicCode(publicCode).build();
		User user = User.builder().id(userId).build();

		// when
		when(storeRepository.findByPublicCodeAndDeletedFalse(publicCode)).thenReturn(java.util.Optional.of(store));
		when(customOAuth2User.getUserId()).thenReturn(10L);
		when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

		doThrow(new UserWaitingLimitExceededException())
			.when(waitingRedisRepository)
			.incrementAndCheckWaitingLimit(10L, 3L);

		// then
		assertThatThrownBy(() -> waitingService.registerWaiting(
			customOAuth2User,
			"ZiVXAD1vVr5b",
			request,
			httpServletRequest
		)).isInstanceOf(UserWaitingLimitExceededException.class);

		verify(reservationRepository, never()).save(any(Reservation.class));
		verify(eventPublisher, never()).publishEvent(any());
		verify(waitingIdempotencyRepository, never()).saveIdempotencyValue(anyString(), any(RegisterWaitingResponse.class));
		verify(waitingRedisRepository).incrementAndCheckWaitingLimit(10L, 3L);
	}

	@Test
	@DisplayName("존재하지 않는 publicCode이면 StoreNotFoundException 발생")
	void registerWaiting_storeNotFound() {
		// given
		when(waitingIdempotencyRepository.findByKey(IDEMPOTENCY_KEY))
			.thenReturn(Optional.empty());
		RegisterWaitingRequest request = new RegisterWaitingRequest(4);

		// when
		when(storeRepository.findByPublicCodeAndDeletedFalse("invalidCode"))
			.thenReturn(Optional.empty());

		// then
		assertThatThrownBy(() -> waitingService.registerWaiting(
			customOAuth2User,
			"invalidCode",
			request,
			httpServletRequest
		)).isInstanceOf(StoreNotFoundException.class);

		verify(userRepository, never()).findById(anyLong());
		verify(waitingRedisRepository, never()).incrementAndCheckWaitingLimit(anyLong(), anyLong());
		verify(reservationRepository, never()).save(any(Reservation.class));
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	@DisplayName("존재하지 않는 userId이면 UserNotFoundException 발생")
	void registerWaiting_userNotFound() {
		// given
		when(waitingIdempotencyRepository.findByKey(IDEMPOTENCY_KEY))
			.thenReturn(Optional.empty());
		RegisterWaitingRequest request = new RegisterWaitingRequest(4);
		String publicCode = "ZiVXAD1vVr5b";

		Store store = Store.builder().publicCode(publicCode).build();

		// when
		when(storeRepository.findByPublicCodeAndDeletedFalse(publicCode))
			.thenReturn(Optional.of(store));
		when(customOAuth2User.getUserId()).thenReturn(10L);
		when(userRepository.findById(10L))
			.thenReturn(Optional.empty());

		// then
		assertThatThrownBy(() -> waitingService.registerWaiting(
			customOAuth2User,
			publicCode,
			request,
			httpServletRequest
		)).isInstanceOf(UserNotFoundException.class);

		verify(waitingRedisRepository, never()).incrementAndCheckWaitingLimit(anyLong(), anyLong());
		verify(reservationRepository, never()).save(any(Reservation.class));
		verify(eventPublisher, never()).publishEvent(any());
	}
}
