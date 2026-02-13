package com.nowait.applicationuser.waiting.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationuser.waiting.dto.CancelWaitingRequest;
import com.nowait.applicationuser.waiting.dto.CancelWaitingResponse;
import com.nowait.applicationuser.waiting.dto.GetMyWaitingInfoResponse;
import com.nowait.applicationuser.waiting.dto.GetWaitingSizeResponse;
import com.nowait.applicationuser.waiting.dto.RegisterWaitingRequest;
import com.nowait.applicationuser.waiting.dto.RegisterWaitingResponse;
import com.nowait.applicationuser.waiting.dto.WaitingCancelIdempotencyValue;
import com.nowait.applicationuser.waiting.dto.WaitingIdempotencyValue;
import com.nowait.applicationuser.waiting.event.AddWaitingRegisterEvent;
import com.nowait.applicationuser.waiting.exception.WorkInProgressException;
import com.nowait.common.enums.ReservationStatus;
import com.nowait.domaincorerdb.department.entity.Department;
import com.nowait.domaincorerdb.department.exception.DepartmentNotFoundException;
import com.nowait.domaincorerdb.department.repository.DepartmentRepository;
import com.nowait.domaincorerdb.reservation.dto.GetMyWaitingBaseDto;
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

	private final IdempotencyService idempotencyService;
	private final ReservationRepository reservationRepository;
	private final WaitingRedisRepository waitingRedisRepository;
	private final StoreRepository storeRepository;
	private final UserRepository userRepository;
	private final DepartmentRepository departmentRepository;
	private final ApplicationEventPublisher eventPublisher;

	/**
	 * 최초 대기 등록
	 */
	// 대기열 리팩토링 서비스 메서드
	@Transactional
	public RegisterWaitingResponse registerWaiting(CustomOAuth2User oAuth2User, String publicCode, RegisterWaitingRequest waitingRequest, HttpServletRequest httpServletRequest) {

		// TODO 멱등키 동시성 처리 로직 고려 필요 (분산락 등)
		Optional<WaitingIdempotencyValue> idempotencyResponse = idempotencyService.validateIdempotency(httpServletRequest);
		if (idempotencyResponse.isPresent() && idempotencyResponse.getClass().equals("IN-PROGRESS")) {
			throw new WorkInProgressException();
		} else if (idempotencyResponse.isPresent() && idempotencyResponse.getClass().equals("COMPLETED")) {
			log.info("Idempotent request detected. Returning existing response.");
			return idempotencyResponse.get().getResponse();
		} else {
			// TODO : DB 저장 실패 시 롤백 처리 필요
			idempotencyService.saveIdempotencyKeyInProgress(httpServletRequest.getHeader("Idempotency-Key"));
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
		idempotencyService.saveIdempotencyResponse(httpServletRequest.getHeader("Idempotency-Key"), response);

		return response;
	}

	@Transactional
	public CancelWaitingResponse cancelWaiting(CustomOAuth2User oAuth2User, String publicCode, CancelWaitingRequest request, HttpServletRequest httpServletRequest) {
		// TODO 멱등키 동시성 처리 로직 고려 필요 (분산락 등)
		WaitingCancelIdempotencyValue idempotencyResponse = idempotencyService.validateIdempotencyCancel(httpServletRequest);
		if (idempotencyResponse != null && idempotencyResponse.getState().equals("IN-PROGRESS")) {
			throw new WorkInProgressException();
		} else if (idempotencyResponse != null && idempotencyResponse.getState().equals("COMPLETED")) {
			log.info("Idempotent request detected. Returning existing response.");
			return idempotencyResponse.getResponse();
		} else {
			idempotencyService.saveIdempotencyKeyInProgress(httpServletRequest.getHeader("Idempotency-Key"));
		}

		Store store = storeRepository.findByPublicCodeAndDeletedFalse(publicCode).orElseThrow(StoreNotFoundException::new);
		Long storeId = store.getStoreId();

		User user = userRepository.findById(oAuth2User.getUserId())
			.orElseThrow(UserNotFoundException::new);

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
		idempotencyService.saveIdempotencyResponse(httpServletRequest.getHeader("Idempotency-Key"), response);

		return response;
	}

	// 웨이팅 목록 조회
	public List<GetMyWaitingInfoResponse> getMyWaitingInfo(CustomOAuth2User oAuth2User) {
		User user = userRepository.findById(oAuth2User.getUserId())
			.orElseThrow(UserNotFoundException::new);

		Long userId = user.getId();

		List<GetMyWaitingBaseDto> waitingInfoList = reservationRepository.findMyWaitingInfo(userId);

		return waitingInfoList.stream()
			.map(dto -> {
				Long storeId = dto.getStoreId();
				Long rank = waitingRedisRepository.getWaitingCount(storeId);

				return GetMyWaitingInfoResponse.builder()
					.waitingNumber(dto.getWaitingNumber())
					.publicCode(dto.getPublicCode())
					.storeId(dto.getStoreId())
					.storeName(dto.getStoreName())
					.departmentName(dto.getDepartmentName())
					.rank(rank.intValue())
					.teamsAhead(rank.intValue() - 1)
					.partySize(dto.getPartySize())
					.status(dto.getStatus().name())
					.registeredAt(dto.getRegisteredAt())
					.location(dto.getLocation())
					.profileImageUrl(dto.getProfileImageUrl())
					.bannerImageUrl(dto.getBannerImageUrl())
					.build();
			})
			.toList();
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
