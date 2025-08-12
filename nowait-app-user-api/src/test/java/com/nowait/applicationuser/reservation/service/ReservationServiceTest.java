package com.nowait.applicationuser.reservation.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import com.nowait.applicationuser.reservation.dto.MyWaitingQueueDto;
import com.nowait.applicationuser.reservation.dto.ReservationCreateRequestDto;
import com.nowait.applicationuser.reservation.dto.ReservationCreateResponseDto;
import com.nowait.applicationuser.reservation.dto.WaitingResponseDto;
import com.nowait.applicationuser.reservation.repository.WaitingUserRedisRepository;
import com.nowait.common.enums.ReservationStatus;
import com.nowait.common.enums.Role;
import com.nowait.domaincorerdb.department.repository.DepartmentRepository;
import com.nowait.domaincorerdb.reservation.entity.Reservation;
import com.nowait.domaincorerdb.reservation.exception.DuplicateReservationException;
import com.nowait.domaincorerdb.reservation.repository.ReservationRepository;
import com.nowait.domaincorerdb.store.entity.Store;
import com.nowait.domaincorerdb.store.exception.StoreNotFoundException;
import com.nowait.domaincorerdb.store.exception.StoreWaitingDisabledException;
import com.nowait.domaincorerdb.store.repository.StoreImageRepository;
import com.nowait.domaincorerdb.store.repository.StoreRepository;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domaincorerdb.user.exception.UserNotFoundException;
import com.nowait.domaincorerdb.user.repository.UserRepository;
import com.nowait.domaincoreredis.reservation.repository.WaitingPermitLuaRepository;
import com.nowait.domainuserrdb.oauth.dto.CustomOAuth2User;

@ExtendWith(MockitoExtension.class)
public class ReservationServiceTest {

	@Mock private StoreRepository storeRepository;
	@Mock private UserRepository userRepository;
	@Mock private WaitingUserRedisRepository waitingRepo;
	@Mock private ReservationRepository reservationRepository;
	@Mock private WaitingPermitLuaRepository waitingPermitLuaRepository;
	@Mock private DepartmentRepository departmentRepository;
	@Mock private StoreImageRepository storeImageRepository;
	@Mock private RedisTemplate<?, ?> redisTemplate;

	@InjectMocks private ReservationService service;


	@Test
	@DisplayName("registerWaiting: 스토어 없음 예외")
	void registerWaiting_StoreNotFound() {
		// Given
		Long storeId = 10L;
		when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

		// When/Then
		assertThrows(StoreNotFoundException.class,
			() -> service.registerWaiting(storeId, mock(CustomOAuth2User.class), null)
		);
	}

	@Test
	@DisplayName("myWaitingInfo: 정상 조회")
	void myWaitingInfo_Success() {
		// Given
		Long storeId = 5L;
		CustomOAuth2User user = mock(CustomOAuth2User.class);
		when(user.getUserId()).thenReturn(20L);
		when(waitingRepo.getRank(storeId, "20")).thenReturn(0L);
		when(waitingRepo.getPartySize(storeId, "20")).thenReturn(3);
		when(waitingRepo.getReservationId(storeId, "20")).thenReturn("5-20250804-0001");

		// When
		WaitingResponseDto info = service.myWaitingInfo(storeId, user);

		// Then
		assertNotNull(info);
		assertEquals("5-20250804-0001", info.getReservationNumber());
		assertEquals(1, info.getRank());
		assertEquals(3, info.getPartySize());
	}

	@Test
	@DisplayName("cancelWaiting: 성공 시 true 반환 및 DB 저장 호출")
	void cancelWaiting_Success() {
		// Given
		Long storeId = 7L;
		CustomOAuth2User user = mock(CustomOAuth2User.class);
		when(user.getUserId()).thenReturn(30L);
		when(waitingRepo.getReservationId(storeId, "30")).thenReturn("RID-1");
		when(waitingRepo.getPartySize(storeId, "30")).thenReturn(4);
		when(waitingRepo.getWaitingTimestamp(storeId, "30")).thenReturn(Instant.now().toEpochMilli());
		when(waitingRepo.removeWaiting(storeId, "30")).thenReturn(true);

		// When
		boolean removed = service.cancelWaiting(storeId, user);

		// Then
		assertTrue(removed);
		verify(reservationRepository).save(any(Reservation.class));
	}

	@Test
	@DisplayName("getAllMyWaitings: active가 비어 있으면 빈 리스트 반환")
	void getAllMyWaitings_Empty_GWT() {
		// Given
		CustomOAuth2User principal = mock(CustomOAuth2User.class);
		when(principal.getUserId()).thenReturn(100L);

		when(waitingPermitLuaRepository.getActiveMembers("100"))
			.thenReturn(Collections.emptySet());

		// When
		List<MyWaitingQueueDto> result = service.getAllMyWaitings(principal);

		// Then
		assertThat(result).isEmpty();

		// 불필요한 스텁/호출이 없도록 보장
		verify(waitingPermitLuaRepository).getActiveMembers("100");
		verifyNoMoreInteractions(waitingPermitLuaRepository);
		// storeRepository 등은 호출되지 않아야 함
		verifyNoInteractions(storeRepository, departmentRepository, storeImageRepository, waitingRepo);
	}


	@Test
	@DisplayName("create(DB): 성공 시 DTO 반환")
	void create_Success() {
		// Given
		Long storeId = 2L;
		CustomOAuth2User user = mock(CustomOAuth2User.class);
		when(user.getUserId()).thenReturn(50L);
		ReservationCreateRequestDto dto = ReservationCreateRequestDto.builder().partySize(5).build();
		Store store = mock(Store.class);
		when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
		when(store.getIsActive()).thenReturn(true);
		User domainUser = mock(User.class);
		when(userRepository.findById(50L)).thenReturn(Optional.of(domainUser));
		when(reservationRepository.existsByUserAndStoreAndStatusIn(eq(domainUser), eq(store), anyList()))
			.thenReturn(false);
		Reservation saved = Reservation.builder()
			.id(99L)
			.store(store)
			.user(domainUser)
			.requestedAt(LocalDateTime.now())
			.status(ReservationStatus.WAITING)
			.partySize(5)
			.build();
		when(reservationRepository.save(any(Reservation.class))).thenReturn(saved);

		// When
		ReservationCreateResponseDto res = service.create(storeId, user, dto);

		// Then
		assertNotNull(res);
		assertEquals(99L, res.getId());
		assertEquals(5, res.getPartySize());
	}

	@Test
	@DisplayName("create(DB): 중복 예약 예외")
	void create_DuplicateException() {
		// Given
		Long storeId = 2L;
		CustomOAuth2User user = mock(CustomOAuth2User.class);
		when(user.getUserId()).thenReturn(50L);
		ReservationCreateRequestDto dto = ReservationCreateRequestDto.builder().partySize(1).build();
		Store store = mock(Store.class);
		when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
		when(store.getIsActive()).thenReturn(true);
		User domainUser = mock(User.class);
		when(userRepository.findById(50L)).thenReturn(Optional.of(domainUser));
		when(reservationRepository.existsByUserAndStoreAndStatusIn(eq(domainUser), eq(store), anyList()))
			.thenReturn(true);

		// When/Then
		assertThrows(DuplicateReservationException.class,
			() -> service.create(storeId, user, dto)
		);
	}

	@Test
	@DisplayName("create(DB): 사용자 없음 예외")
	void create_UserNotFound() {
		// Given
		Long storeId = 2L;
		CustomOAuth2User user = mock(CustomOAuth2User.class);
		when(user.getUserId()).thenReturn(50L);
		when(storeRepository.findById(storeId)).thenReturn(Optional.of(mock(Store.class)));
		when(userRepository.findById(50L)).thenReturn(Optional.empty());

		// When/Then
		assertThrows(UserNotFoundException.class,
			() -> service.create(storeId, user, ReservationCreateRequestDto.builder().partySize(1).build())
		);
	}

	@Test
	@DisplayName("create(DB): 스토어 없음 예외")
	void create_StoreNotFound() {
		// Given
		Long storeId = 3L;
		when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

		// When/Then
		assertThrows(StoreNotFoundException.class,
			() -> service.create(storeId, mock(CustomOAuth2User.class), ReservationCreateRequestDto.builder().partySize(1).build())
		);
	}

	@Test
	@DisplayName("create(DB): 스토어 비활성화 예외")
	void create_StoreDisabledException() {
		// Given
		Long storeId = 4L;
		CustomOAuth2User user = mock(CustomOAuth2User.class);
		when(user.getUserId()).thenReturn(60L);

		// 1) 사용자 조회 스텁 추가
		User domainUser = mock(User.class);
		when(userRepository.findById(60L)).thenReturn(Optional.of(domainUser));

		// 2) 스토어 조회 및 비활성화 스텁
		Store store = mock(Store.class);
		when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
		when(store.getIsActive()).thenReturn(false);

		// When / Then
		assertThrows(StoreWaitingDisabledException.class,
			() -> service.create(
				storeId,
				user,
				ReservationCreateRequestDto.builder().partySize(2).build()
			)
		);
	}

	@Test
	@DisplayName("registerWaiting: 성공 시 WaitingResponseDto 반환")
	void registerWaiting_Success_GWT() {
		// Given
		Long storeId = 10L;
		CustomOAuth2User principal = mock(CustomOAuth2User.class);
		when(principal.getUserId()).thenReturn(100L);

		ReservationCreateRequestDto dto = ReservationCreateRequestDto.builder().partySize(2).build();

		Store store = mock(Store.class);
		when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
		when(store.getIsActive()).thenReturn(true);

		User domainUser = mock(User.class);
		when(userRepository.findById(100L)).thenReturn(Optional.of(domainUser));
		when(domainUser.getRole()).thenReturn(Role.USER);
		// 서비스가 user.getId()를 쓰는 경우 필수
		when(domainUser.getId()).thenReturn(100L);

		when(waitingRepo.calculateTTLUntilNext03AM()).thenReturn(Duration.ofHours(1));
		when(waitingRepo.isUserWaiting(storeId, "100")).thenReturn(false);
		when(waitingPermitLuaRepository.acquireLease(eq("100"), anyString(), anyLong(), anyLong(), eq(3), any()))
			.thenReturn(true);

		when(waitingRepo.addToWaitingQueue(eq(storeId), eq("100"), eq(2), anyLong()))
			.thenReturn("10-20250804-0001");
		when(waitingRepo.getRank(storeId, "100")).thenReturn(4L);

		// When
		WaitingResponseDto result = service.registerWaiting(storeId, principal, dto);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getReservationNumber()).isEqualTo("10-20250804-0001");
		assertThat(result.getRank()).isEqualTo(5);
		assertThat(result.getPartySize()).isEqualTo(2);

		verify(waitingPermitLuaRepository).acquireLease(eq("100"), anyString(), anyLong(), anyLong(), eq(3), any());
		verify(waitingRepo).addToWaitingQueue(eq(storeId), eq("100"), eq(2), anyLong());
		verify(waitingPermitLuaRepository).finalizeActive(eq("100"), anyString(), eq(String.valueOf(storeId)), eq("10-20250804-0001"), any());
		verifyNoMoreInteractions(waitingPermitLuaRepository, waitingRepo);
	}


	@Test
	@DisplayName("registerWaiting: 동일 매장 재요청 시 기존 정보 반환(임대 미소비)")
	void registerWaiting_Duplicate_SameStore_GWT() {
		// Given
		Long storeId = 10L;
		CustomOAuth2User principal = mock(CustomOAuth2User.class);
		when(principal.getUserId()).thenReturn(100L);

		ReservationCreateRequestDto dto = ReservationCreateRequestDto.builder().partySize(2).build();

		Store store = mock(Store.class);
		when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
		when(store.getIsActive()).thenReturn(true);

		User domainUser = mock(User.class);
		when(userRepository.findById(100L)).thenReturn(Optional.of(domainUser));
		when(domainUser.getRole()).thenReturn(Role.USER);
		when(domainUser.getId()).thenReturn(100L);

		// 이미 해당 매장에 있는 상태
		when(waitingRepo.isUserWaiting(storeId, "100")).thenReturn(true);
		when(waitingRepo.getRank(storeId, "100")).thenReturn(4L);
		when(waitingRepo.getPartySize(storeId, "100")).thenReturn(2);
		when(waitingRepo.getReservationId(storeId, "100")).thenReturn("10-20250804-0001");

		// When
		WaitingResponseDto result = service.registerWaiting(storeId, principal, dto);

		// Then
		assertThat(result.getReservationNumber()).isEqualTo("10-20250804-0001");
		assertThat(result.getRank()).isEqualTo(5);
		assertThat(result.getPartySize()).isEqualTo(2);

		verify(waitingPermitLuaRepository, never()).acquireLease(any(), any(), anyLong(), anyLong(), anyInt(), any());
		verify(waitingRepo, never()).addToWaitingQueue(anyLong(), anyString(), anyInt(), anyLong());
	}


	@Test
	@DisplayName("registerWaiting: 유저 한도(3개) 초과 시 예외")
	void registerWaiting_LimitExceeded_GWT() {
		// Given
		Long storeId = 40L;
		CustomOAuth2User principal = mock(CustomOAuth2User.class);
		when(principal.getUserId()).thenReturn(100L);

		ReservationCreateRequestDto dto = ReservationCreateRequestDto.builder().partySize(2).build();

		Store store = mock(Store.class);
		when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
		when(store.getIsActive()).thenReturn(true);

		User domainUser = mock(User.class);
		when(userRepository.findById(100L)).thenReturn(Optional.of(domainUser));
		when(domainUser.getRole()).thenReturn(Role.USER);
		when(domainUser.getId()).thenReturn(100L);

		when(waitingRepo.calculateTTLUntilNext03AM()).thenReturn(Duration.ofHours(1));
		when(waitingRepo.isUserWaiting(storeId, "100")).thenReturn(false);
		// 임대 3회 모두 실패하도록
		when(waitingPermitLuaRepository.acquireLease(eq("100"), anyString(), anyLong(), anyLong(), eq(3), any()))
			.thenReturn(false);

		// When & Then
		assertThatThrownBy(() -> service.registerWaiting(storeId, principal, dto))
			.isInstanceOf(com.nowait.domaincorerdb.reservation.exception.UserWaitingLimitExceededException.class)
			.hasMessageContaining("유저당 웨이팅 가능 개수");

		verify(waitingRepo, never()).addToWaitingQueue(anyLong(), anyString(), anyInt(), anyLong());
		verify(waitingPermitLuaRepository, atLeastOnce()).acquireLease(eq("100"), anyString(), anyLong(), anyLong(), eq(3), any());
	}
}
