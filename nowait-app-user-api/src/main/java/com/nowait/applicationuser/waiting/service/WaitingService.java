package com.nowait.applicationuser.waiting.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationuser.waiting.dto.CancelWaitingRequest;
import com.nowait.applicationuser.waiting.dto.CancelWaitingResponse;
import com.nowait.applicationuser.waiting.dto.GetWaitingSizeResponse;
import com.nowait.applicationuser.waiting.dto.RegisterWaitingRequest;
import com.nowait.applicationuser.waiting.dto.RegisterWaitingResponse;
import com.nowait.applicationuser.waiting.dto.WaitingIdempotencyValue;
import com.nowait.applicationuser.waiting.event.AddWaitingRegisterEvent;
import com.nowait.applicationuser.waiting.redis.WaitingIdempotencyRepository;
import com.nowait.common.enums.ReservationStatus;
import com.nowait.domaincorerdb.department.entity.Department;
import com.nowait.domaincorerdb.department.exception.DepartmentNotFoundException;
import com.nowait.domaincorerdb.department.repository.DepartmentRepository;
import com.nowait.domaincorerdb.reservation.entity.Reservation;
import com.nowait.domaincorerdb.reservation.exception.ReservationNotFoundException;
import com.nowait.domaincorerdb.reservation.repository.ReservationRepository;
import com.nowait.domaincorerdb.store.entity.Store;
import com.nowait.domaincorerdb.store.exception.StoreNotFoundException;
import com.nowait.domaincorerdb.store.repository.StoreRepository;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domaincorerdb.user.exception.UserNotFoundException;
import com.nowait.domaincorerdb.user.repository.UserRepository;
import com.nowait.domaincoreredis.common.util.RedisKeyUtils;
import com.nowait.domaincoreredis.reservation.repository.WaitingRedisRepository;
import com.nowait.domainuserrdb.oauth.dto.CustomOAuth2User;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaitingService {

	private final ReservationRepository reservationRepository;
	private final WaitingRedisRepository waitingRedisRepository;
	private final StoreRepository storeRepository;
	private final UserRepository userRepository;
	private final DepartmentRepository departmentRepository;
	private final ApplicationEventPublisher eventPublisher;
	private final WaitingIdempotencyRepository waitingIdempotencyRepository;

	/**
	 * 최초 대기 등록
	 * @param publicCode
	 * @param waitingRequest
	 */
	// 대기열 리팩토링 서비스 메서드
	@Transactional
	public RegisterWaitingResponse registerWaiting(CustomOAuth2User oAuth2User, String publicCode, RegisterWaitingRequest waitingRequest, HttpServletRequest httpServletRequest) {

		// TODO 멱등키 동시성 처리 로직 고려 필요 (분산락 등)
		RegisterWaitingResponse registerWaitingResponse = validateIdempotency(httpServletRequest);
		if (registerWaitingResponse != null) {
			log.info("Idempotent request detected. Returning existing response.");
			return registerWaitingResponse;
		}

		// TODO 유저 및 주점 존재 검증은 공통으로 많이 쓰이니 AOP로 빼는게 좋을 듯
		Store store = storeRepository.findByPublicCodeAndDeletedFalse(publicCode)
			.orElseThrow(StoreNotFoundException::new);

		User user = userRepository.findById(oAuth2User.getUserId())
			.orElseThrow(UserNotFoundException::new);

		// 일일 가능 웨이팅 최대 개수 초과 검증
		// TODO race condition 발생 가능성 점검 필요, DB 저장 로직 실패 시 롤백 처리 필요
		waitingRedisRepository.incrementAndCheckWaitingLimit(user.getId(), 3L);

		// 웨이팅 고유 번호 생성 - YYYYMMDD-storeId-sequence number 일련 번호
		Long storeId = store.getStoreId();
		LocalDateTime timestamp = LocalDateTime.now();
		String waitingNumber = generateWaitingNumber(storeId, timestamp);

		// DB에 상태 값 저장
		Reservation reservation = Reservation.builder()
			.reservationNumber(waitingNumber)
			.store(store)
			.user(user)
			.status(ReservationStatus.WAITING)
			.partySize(waitingRequest.getPartySize())
			.requestedAt(timestamp)
			.updatedAt(timestamp)
			.build();

		reservationRepository.save(reservation);

		// Redis 대기열 추가 이벤트 발행
		eventPublisher.publishEvent(
			new AddWaitingRegisterEvent(
				storeId,
				user.getId(),
				timestamp
			)
		);

		RegisterWaitingResponse response = RegisterWaitingResponse.builder()
			.waitingNumber(waitingNumber)
			.partySize(waitingRequest.getPartySize())
			.build();

		// TODO 멱등키 응답 실패 시 어떻게 처리할 지 점검 필요
		saveIdempotencyResponse(httpServletRequest.getHeader("Idempotency-Key"), response);

		return response;
	}

	@Transactional
	public CancelWaitingResponse cancelWaiting(CustomOAuth2User oAuth2User, String publicCode, CancelWaitingRequest request) {

		Store store = storeRepository.findByPublicCodeAndDeletedFalse(publicCode).orElseThrow(StoreNotFoundException::new);
		Long storeId = store.getStoreId();

		User user = userRepository.findById(oAuth2User.getUserId())
			.orElseThrow(UserNotFoundException::new);

		// TODO 멱등키 검증 로직 점검 필요
		// Optional<WaitingIdempotencyValue> existingIdempotencyValue = waitingIdempotencyRepository.findByKey(idempotentKey);
		// if (existingIdempotencyValue.isPresent()) {
		// 	log.info("Existing idempotency key found: {}", idempotentKey);
		// 	return existingIdempotencyValue.get().getResponse();
		// }

		// DB 웨이팅 상태 취소 처리
		Reservation reservation = reservationRepository.findReservationByReservationNumber(request.getWaitingNumber())
			.orElseThrow(ReservationNotFoundException::new);

		reservation.markAsCancelled(LocalDateTime.now());

		// Redis 대기열 취소 이벤트 발행
		waitingRedisRepository.removeWaiting(storeId, user.getId());

		CancelWaitingResponse response = CancelWaitingResponse.builder()
			.waitingNumber(reservation.getReservationNumber())
			.storeId(storeId)
			.reservationStatus(reservation.getStatus())
			.canceledAt(reservation.getUpdatedAt())
			.message("대기 취소가 완료되었습니다.")
			.build();

		// 멱등키가 있다면 멱등 응답 저장
		// waitingIdempotencyRepository.saveIdempotencyValue(idempotentKey, response);

		return response;
	}

	// 멱등키 검증 메서드
	private RegisterWaitingResponse validateIdempotency(HttpServletRequest httpServletRequest) {
		String idempotentKey = httpServletRequest.getHeader("Idempotency-Key");

		// 멱등키 검증 - 이미 동일한 멱등키로 등록된 웨이팅이 있는지 확인
		// TODO 멱등성 검증 로직 점검 필요
		return waitingIdempotencyRepository.findByKey(idempotentKey)
			.map(WaitingIdempotencyValue::getResponse)
			.orElse(null);
	}

	// 멱등키 응답 저장 메서드
	private void saveIdempotencyResponse(String idempotentKey, RegisterWaitingResponse response) {
		if (idempotentKey != null && !idempotentKey.isBlank()) {
			waitingIdempotencyRepository.saveIdempotencyValue(idempotentKey, response);
		}
	}

	// 현재 대기 인원 수 조회
	public GetWaitingSizeResponse getWaitingCount(CustomOAuth2User oAuth2User, String publicCode) {

		Store store = storeRepository.findByPublicCodeAndDeletedFalse(publicCode)
			.orElseThrow(StoreNotFoundException::new);

		User user = userRepository.findById(oAuth2User.getUserId())
			.orElseThrow(UserNotFoundException::new);

		Department department = departmentRepository.findById(store.getDepartmentId())
			.orElseThrow(DepartmentNotFoundException::new);

		Long storeId = store.getStoreId();

		Long waitingCount = waitingRedisRepository.getWaitingCount(storeId);

		return GetWaitingSizeResponse.builder()
			.storeId(storeId)
			.storeName(store.getName())
			.departmentName(department.getName())
			.waitingCount(waitingCount)
			.build();
	}

	private String generateWaitingNumber(Long storeId, LocalDateTime timestamp) {
		// 1) 키 접두사 - 날짜
		String today = timestamp.format(DateTimeFormatter.BASIC_ISO_DATE); // YYYYMMDD

		// atomic increment
		// TODO 웨이팅 실패 시 카운터 롤백 처리 필요
		String dailySeqKey = RedisKeyUtils.buildWaitingSeqKey(storeId) + ":" + today; // ex. waiting:sequence:{storeId}:{today}
		Long seqNum = waitingRedisRepository.incrementDailySequence(dailySeqKey);

		// 3) 4자리 0패딩
		String seqStr = String.format("%04d", seqNum);

		// 4) 최종 ID 조합
		return today + "-" + storeId + "-" + seqStr;
	}
}
