// package com.nowait.applicationuser.reservation.service;
//
// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.anyLong;
// import static org.mockito.Mockito.*;
//
// import java.util.ArrayList;
// import java.util.Collections;
// import java.util.HashSet;
// import java.util.List;
// import java.util.Optional;
// import java.util.Set;
// import java.util.concurrent.CountDownLatch;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.Executors;
// import java.util.concurrent.TimeUnit;
//
// import org.junit.jupiter.api.BeforeAll;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
// import org.springframework.data.redis.core.StringRedisTemplate;
// import org.testcontainers.junit.jupiter.Container;
// import org.testcontainers.junit.jupiter.Testcontainers;
//
// import com.nowait.applicationuser.reservation.dto.ReservationCreateRequestDto;
// import com.nowait.applicationuser.reservation.dto.WaitingResponseDto;
// import com.nowait.applicationuser.reservation.repository.WaitingUserRedisRepository;
// import com.nowait.common.enums.Role;
// import com.nowait.domaincoreredis.common.util.RedisKeyUtils;
// import com.nowait.domaincorerdb.department.repository.DepartmentRepository;
// import com.nowait.domaincorerdb.reservation.repository.ReservationRepository;
// import com.nowait.domaincorerdb.store.repository.StoreImageRepository;
// import com.nowait.domaincorerdb.store.repository.StoreRepository;
// import com.nowait.domaincorerdb.user.repository.UserRepository;
// import com.nowait.domaincorerdb.store.entity.Store;
// import com.nowait.domaincorerdb.user.entity.User;
// import com.nowait.domaincoreredis.reservation.repository.WaitingPermitLuaRepository;
// import com.nowait.domainuserrdb.oauth.dto.CustomOAuth2User;
// import com.redis.testcontainers.RedisContainer;
//
// @Testcontainers
// class ReservationServiceConcurrencyTest {
//
// 	@Container
// 	static RedisContainer redis = new RedisContainer("redis:6.2.6");
//
// 	static StringRedisTemplate redisTemplate;
// 	static WaitingUserRedisRepository waitingRepo;
// 	static ReservationService reservationService;
//
// 	// Mockito로 대체할 의존성들
// 	static ReservationRepository reservationRepo;
// 	static StoreRepository storeRepo;
// 	static UserRepository userRepo;
// 	static DepartmentRepository deptRepo;
// 	static StoreImageRepository storeImageRepo;
// 	static WaitingPermitLuaRepository waitingPermitRepo;
//
// 	private static final Long STORE_ID = 100L;
// 	private static final int THREAD_COUNT = 50;
//
// 	@BeforeAll
// 	static void setupAll() {
// 		// 1) RedisTemplate 초기화
// 		LettuceConnectionFactory factory = new LettuceConnectionFactory(
// 			redis.getHost(),
// 			redis.getFirstMappedPort()
// 		);
// 		factory.afterPropertiesSet();
// 		redisTemplate = new StringRedisTemplate(factory);
//
// 		// 2) 실제 Redis 리포지토리 객체 생성
// 		waitingRepo = new WaitingUserRedisRepository(redisTemplate);
// 		waitingPermitRepo = new WaitingPermitLuaRepository(redisTemplate);
//
// 		// 3) Mockito mock 인스턴스 생성
// 		reservationRepo = mock(ReservationRepository.class);
// 		storeRepo = mock(StoreRepository.class);
// 		userRepo = mock(UserRepository.class);
// 		deptRepo = mock(DepartmentRepository.class);
// 		storeImageRepo = mock(StoreImageRepository.class);
//
// 		// 4) store/user 유효성 검증 스텁
// 		Store mockStore = mock(Store.class);
// 		when(mockStore.getIsActive()).thenReturn(true);
// 		when(storeRepo.findById(anyLong())).thenReturn(Optional.of(mockStore));
//
// 		when(userRepo.findById(anyLong())).thenAnswer(inv -> {
// 			Long uid = inv.getArgument(0, Long.class);
// 			User u = mock(User.class);
// 			when(u.getRole()).thenReturn(Role.USER);
// 			when(u.getId()).thenReturn(uid);
// 			return Optional.of(u);
// 		});
//
// 		// 5) ReservationService 실체 생성
// 		reservationService = new ReservationService(
// 			reservationRepo,
// 			storeRepo,
// 			userRepo,
// 			waitingRepo,
// 			deptRepo,
// 			storeImageRepo,
// 			waitingPermitRepo,
// 			redisTemplate
// 		);
// 	}
//
// 	@BeforeEach
// 	void clearRedis() {
// 		// 매 테스트마다 Redis 키 초기화
// 		redisTemplate.delete(redisTemplate.keys(RedisKeyUtils.buildWaitingKeyPrefix() + "*"));
// 		redisTemplate.delete(redisTemplate.keys(RedisKeyUtils.buildWaitingPartySizeKeyPrefix() + "*"));
// 		redisTemplate.delete(redisTemplate.keys(RedisKeyUtils.buildWaitingStatusKeyPrefix() + "*"));
// 		redisTemplate.delete(redisTemplate.keys(RedisKeyUtils.buildReservationSeqKey(STORE_ID) + ":*"));
// 		redisTemplate.delete(redisTemplate.keys(RedisKeyUtils.buildReservationNumberKey(STORE_ID) + ":*"));
// 		redisTemplate.delete(redisTemplate.keys("waiting:user:*"));
// 		redisTemplate.delete(RedisKeyUtils.buildReservationNumberKey(STORE_ID));
// 		redisTemplate.delete(redisTemplate.keys(RedisKeyUtils.buildReservationSeqKey(STORE_ID) + ":*"));
// 	}
//
// 	@Test
// 	@DisplayName("동시 50명 대기 등록 테스트 (Given–When–Then)")
// 	void concurrentRegisterWaiting() throws InterruptedException {
// 		// --- Given ---
// 		// 50개 스레드를 준비하고, 동시에 시작/종료를 제어할 CountDownLatch
// 		ExecutorService exec = Executors.newFixedThreadPool(THREAD_COUNT);
// 		CountDownLatch startLatch = new CountDownLatch(1);
// 		CountDownLatch finishLatch = new CountDownLatch(THREAD_COUNT);
// 		// 결과를 수집할 스레드 안전 리스트
// 		List<WaitingResponseDto> responses = Collections.synchronizedList(new ArrayList<>());
// 		List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
//
// 		// THREAD_COUNT개의 작업을 스레드풀에 제출
// 		for (int i = 0; i < THREAD_COUNT; i++) {
// 			final long uid = 1000 + i;
// 			exec.submit(() -> {
// 				try {
// 					// 모든 스레드가 startLatch.await() 대기 중
// 					startLatch.await();
//
// 					// 각 스레드마다 OAuth2User mocking
// 					CustomOAuth2User user = mock(CustomOAuth2User.class);
// 					when(user.getUserId()).thenReturn(uid);
//
// 					// 실제 호출
// 					ReservationCreateRequestDto dto = ReservationCreateRequestDto.builder()
// 						.partySize(2)
// 						.build();
// 					WaitingResponseDto responseDto = reservationService.registerWaiting(STORE_ID, user, dto);
// 					responses.add(responseDto);
// 				} catch (Throwable t) {
// 					errors.add(t);
// 				} finally {
// 					// 작업 완료 신고
// 					finishLatch.countDown();
// 				}
// 			});
// 		}
//
// 		// --- When ---
// 		// 모든 스레드를 동시에 시작
// 		startLatch.countDown();
// 		// 최대 15초 대기
// 		boolean completedInTime = finishLatch.await(15, TimeUnit.SECONDS);
//
// 		// --- Then ---
// 		// 1) 모든 스레드가 제시간에 완료되었는가?
// 		assertTrue(completedInTime, "스레드가 제시간에 완료되지 않았습니다.");
// 		assertTrue(errors.isEmpty(), "스레드 예외 발생: " + errors);
// 		// 2) 반환된 DTO 개수가 50개인가?
// 		assertEquals(THREAD_COUNT, responses.size(), "전체 응답 수 불일치");
//
// 		// 3) 예약번호가 모두 유니크한가?
// 		Set<String> reservationIds = new HashSet<>();
// 		for (WaitingResponseDto r : responses) {
// 			assertNotNull(r.getReservationNumber(), "예약번호가 null 입니다");
// 			reservationIds.add(r.getReservationNumber());
// 		}
// 		assertEquals(THREAD_COUNT, reservationIds.size(), "예약번호가 중복되었습니다");
//
// 		// 스레드풀 정리
// 		exec.shutdown();
// 	}
//
// }
