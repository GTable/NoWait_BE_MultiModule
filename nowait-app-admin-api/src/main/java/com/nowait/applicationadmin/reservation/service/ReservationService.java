package com.nowait.applicationadmin.reservation.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationadmin.reservation.dto.CallGetResponseDto;
import com.nowait.applicationadmin.reservation.dto.EntryStatusResponseDto;
import com.nowait.applicationadmin.reservation.dto.ReservationGetResponseDto;
import com.nowait.applicationadmin.reservation.dto.ReservationStatusSummaryDto;
import com.nowait.applicationadmin.reservation.dto.ReservationStatusUpdateRequestDto;
import com.nowait.applicationadmin.reservation.dto.WaitingUserResponse;
import com.nowait.common.enums.ReservationStatus;
import com.nowait.common.enums.Role;
import com.nowait.domaincorerdb.reservation.entity.Reservation;
import com.nowait.domaincorerdb.reservation.exception.InvalidReservationParameterException;
import com.nowait.domaincorerdb.reservation.exception.InvalidReservationStatusTransitionException;
import com.nowait.domaincorerdb.store.exception.StoreViewUnauthorizedException;
import com.nowait.domaincoreredis.reservation.exception.ReservationDataInconsistencyException;
import com.nowait.domaincorerdb.reservation.exception.ReservationNotFoundException;
import com.nowait.domaincorerdb.reservation.exception.ReservationUpdateUnauthorizedException;
import com.nowait.domaincorerdb.reservation.exception.ReservationViewUnauthorizedException;
import com.nowait.domaincorerdb.reservation.exception.UnsupportedReservationStatusException;
import com.nowait.domaincorerdb.reservation.repository.ReservationRepository;
import com.nowait.domaincorerdb.store.repository.StoreRepository;
import com.nowait.domaincorerdb.user.entity.MemberDetails;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domaincorerdb.user.exception.UserNotFoundException;
import com.nowait.domaincorerdb.user.repository.UserRepository;
import com.nowait.domaincoreredis.common.util.RedisKeyUtils;
import com.nowait.domaincoreredis.reservation.repository.WaitingPermitLuaRepository;
import com.nowait.domaincoreredis.reservation.repository.WaitingRedisRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationService {

	private final ReservationRepository reservationRepository;
	private final UserRepository userRepository;
	private final WaitingRedisRepository waitingRedisRepository;
	private final StoreRepository storeRepository;
	private final WaitingPermitLuaRepository waitingPermitLuaRepository;
	private final RedisTemplate redisTemplate;

	//TODO 성능 비교를 위해 남겨둔 로직
	@Transactional(readOnly = true)
	public ReservationStatusSummaryDto getReservationListByStoreId(Long storeId, MemberDetails memberDetails) {
		User user = getUser(memberDetails);
		validateViewAuthorization(user, storeId);
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
		User user = getUser(memberDetails);
		Reservation reservation = reservationRepository.findById(reservationId)
			.orElseThrow(ReservationNotFoundException::new);
		validateUpdateAuthorization(user, reservation.getStore().getStoreId());

		reservation.markUpdated(LocalDateTime.now(), requestDto.getStatus());
		return CallGetResponseDto.fromEntity(reservation);
	}

	// Redis queue에 있는 주점별 전체 대기열 조회
	//TODO 개선필요 -> createAt 정확성 및 reservationhId 생성 방법(예약생성부터 DB에 박아야하나....)
	@Transactional(readOnly = true)
	public List<WaitingUserResponse> getAllWaitingUserDetails(Long storeId) {

		// 1) Redis로부터 전체 대기 userId + score(등록 시각)
		List<ZSetOperations.TypedTuple<String>> waitingList = waitingRedisRepository.getAllWaitingWithScore(storeId);
		System.out.println(waitingList);
		if (waitingList.isEmpty()) {
			return List.of();
		}

		// 2) userId 목록 분리
		List<String> userIds = waitingList.stream()
			.map(ZSetOperations.TypedTuple::getValue)
			.toList();

		// 3) User 닉네임을 한 번에 배치 조회
		List<Long> userIdLongs = userIds.stream()
			.map(Long::valueOf)
			.toList();

		List<User> userList = userRepository.findByIdIn(userIdLongs);

		Map<String, String> nicknameMap = new HashMap<>(userList.size());
		Map<String, String> phoneNumberMap = new HashMap<>(userList.size());

		for (User user : userList) {
			String key = user.getId().toString();
			nicknameMap.put(key, user.getNickname());
			phoneNumberMap.put(key, user.getPhoneNumber());
		}

		// 4) Redis 파이프라인: partySize, status, reservationId
		String pk = RedisKeyUtils.buildWaitingPartySizeKeyPrefix() + storeId;
		String sk = RedisKeyUtils.buildWaitingStatusKeyPrefix() + storeId;
		String nk = RedisKeyUtils.buildReservationNumberKey(storeId);
		String cak = RedisKeyUtils.buildWaitingCalledAtKeyPrefix() + storeId;
		String qk = RedisKeyUtils.buildWaitingKeyPrefix() + storeId;

		List<Object> pipeline = redisTemplate.executePipelined((RedisCallback<Object>)conn -> {
			byte[] uid;
			for (String userId : userIds) {
				uid = redisTemplate.getStringSerializer().serialize(userId);
				conn.hGet(pk.getBytes(), uid);   // partySize
				conn.hGet(sk.getBytes(), uid);   // status
				conn.hGet(nk.getBytes(), uid);   // reservationId
				conn.hGet(cak.getBytes(), uid);  // calledAt
				conn.zScore(qk.getBytes(), uid); // score (등록 시각)
			}
			return null;
		});

		// 5) 결과 매핑
		List<WaitingUserResponse> result = new ArrayList<>(userIds.size());
		Iterator<Object> it = pipeline.iterator();
		ZoneId zone = ZoneId.of("Asia/Seoul");

		for (ZSetOperations.TypedTuple<String> tuple : waitingList) {
			String userId = tuple.getValue();
			Integer partySize = Optional.ofNullable((String)it.next()).map(Integer::valueOf).orElse(0);
			String status = (String)it.next();
			String reservationId = (String)it.next();
			String calledAtStr = (String)it.next();
			Double score = (Double)it.next();

			// score → createdAt
			LocalDateTime createdAt = score != null
				? Instant.ofEpochMilli(score.longValue()).atZone(zone).toLocalDateTime()
				: null;

			LocalDateTime calledAt = calledAtStr != null
				? Instant.ofEpochMilli(Long.parseLong(calledAtStr)).atZone(zone).toLocalDateTime()
				: null;

			String userName = nicknameMap.getOrDefault(userId, "Unknown");
			String phoneNumber = phoneNumberMap.getOrDefault(userId, "Unknown");

			result.add(
				WaitingUserResponse.fromRedis(reservationId, userId, phoneNumber, partySize, userName, createdAt,
					calledAt, status,
					score));
		}

		return result;
	}

	// 완료 or 취소 처리된 대기 리스트 조회
	@Transactional(readOnly = true)
	public List<WaitingUserResponse> getCompletedWaitingUserDetails(Long storeId, MemberDetails memberDetails) {
		User user = getUser(memberDetails);
		validateViewAuthorization(user, storeId);
		List<Reservation> reservations = findTodayWaiting(storeId);

		return reservations.stream()
			.map(WaitingUserResponse::fromEntity)
			.toList();
	}

	/**
	 * 상태를 하나의 메서드에서 처리합니다.
	 * - CALLING   : Redis 상태를 CALLING으로 변경 → DB 저장 → calledAt 반환
	 * - CONFIRMED : Redis에서 삭제             → DB 저장 → 완료 메시지 반환
	 * - CANCELLED : Redis에서 삭제             → DB 저장 → 취소 메시지 반환
	 */
	@Transactional
	public EntryStatusResponseDto processEntryStatus(Long storeId, String userId, MemberDetails member,
		ReservationStatus newStatus) {

		User user = getUser(member);
		validateUpdateAuthorization(user, storeId);

		if (userId == null || userId.isBlank()) {
			throw new InvalidReservationParameterException("userId 값이 비어있습니다.");
		}

		String queueKey = RedisKeyUtils.buildWaitingKeyPrefix() + storeId;

		// Redis에서 상태·score·partySize·calledAt 조회
		String reservationNumber = waitingRedisRepository.getReservationId(storeId, userId);
		String currStatus = waitingRedisRepository.getWaitingStatus(storeId, userId);
		Double score = redisTemplate.opsForZSet().score(queueKey, userId);
		Integer partySize = waitingRedisRepository.getWaitingPartySize(storeId, userId);

		if (partySize == null || partySize <= 0) {
			throw new InvalidReservationParameterException("partySize가 유효하지 않습니다. (storeId=" + storeId + ", userId=" + userId + ")");
		}

		if (reservationNumber == null || currStatus == null) {
			throw new ReservationDataInconsistencyException(
				String.format("storeId=%d, userId=%s, reservationNumber=%s, status=%s, partySize=%s",
					storeId, userId, reservationNumber, currStatus, partySize)
			);
		}

		LocalDateTime requestedAt = score != null
			? Instant.ofEpochMilli(score.longValue()).atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime()
			: LocalDateTime.now();
		LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

		switch (newStatus) {
			case CALLING:
				if (!ReservationStatus.WAITING.name().equals(currStatus)) {
					throw new InvalidReservationStatusTransitionException(
						ReservationStatus.valueOf(currStatus), ReservationStatus.CALLING
					);
				}
				waitingRedisRepository.setWaitingStatus(storeId, userId, ReservationStatus.CALLING.name());
				waitingRedisRepository.setWaitingCalledAt(storeId, userId,
					now.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli());

				return EntryStatusResponseDto.builder()
					.reservationNumber(reservationNumber)
					.userId(userId)
					.partySize(partySize)
					.userName(userRepository.getReferenceById(Long.valueOf(userId)).getNickname())
					.createdAt(requestedAt)
					.status("CALLING")
					.updatedAt(now)
					.message("호출되었습니다.")
					.build();

			case CONFIRMED:
				// 1) 기존 대기 중이거나 호출 중일 때: Redis → DB 최초 저장
				if (ReservationStatus.WAITING.name().equals(currStatus) || ReservationStatus.CALLING.name()
					.equals(currStatus)) {

					if (reservationNumber != null) {
						waitingPermitLuaRepository.removeActiveMember(
							userId, String.valueOf(storeId), reservationNumber
						);
					}

					// 새 Reservation 생성 & 저장
					Reservation r = Reservation.builder()
						.reservationNumber(reservationNumber)
						.store(storeRepository.getReferenceById(storeId))
						.user(userRepository.getReferenceById(Long.valueOf(userId)))
						.partySize(partySize)
						.requestedAt(requestedAt)
						.updatedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
						.build();

					// 호출 시각 반영
					r.markUpdated(LocalDateTime.now(), ReservationStatus.CONFIRMED);
					Reservation saved = reservationRepository.save(r);
					// Redis 전부 삭제
					waitingRedisRepository.deleteWaiting(storeId, userId);
					return EntryStatusResponseDto.fromEntity(saved);
				} else {
					if (reservationNumber != null) {
						try {
							waitingPermitLuaRepository.removeActiveMember(userId, String.valueOf(storeId),
								reservationNumber);
						} catch (Exception ignore) {
						}
					}

					// 2) 이미 취소(CANCELLED)된 경우: DB 레코드 찾아 바로 CONFIRMED 로 전환
					// TODO 메서드로 분리
					LocalDateTime start = LocalDate.now().atStartOfDay();
					LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);
					Reservation existing = reservationRepository
						.findFirstByStore_StoreIdAndUserIdAndStatusInAndRequestedAtBetweenOrderByRequestedAtDesc(
							storeId,
							Long.valueOf(userId),
							List.of(ReservationStatus.CANCELLED),
							start,
							end
						).orElseThrow(() -> new ReservationDataInconsistencyException(
							String.format("취소된 예약이 DB에 존재하지 않습니다. (storeId=%d, userId=%s)", storeId, userId)
						));

					existing.markUpdated(LocalDateTime.now(ZoneId.of("Asia/Seoul")), ReservationStatus.CONFIRMED);
					Reservation saved = reservationRepository.save(existing);
					return EntryStatusResponseDto.fromEntity(saved);
				}

			case CANCELLED:
				if (!(ReservationStatus.WAITING.name().equals(currStatus)
					  || ReservationStatus.CALLING.name().equals(currStatus))) {
					throw new InvalidReservationStatusTransitionException(
						ReservationStatus.valueOf(currStatus), ReservationStatus.CANCELLED
					);
				}

				if (reservationNumber != null) {
					waitingPermitLuaRepository.removeActiveMember(
						userId, String.valueOf(storeId), reservationNumber
					);
				}

				Reservation r = Reservation.builder()
					.reservationNumber(reservationNumber)
					.store(storeRepository.getReferenceById(storeId))
					.user(userRepository.getReferenceById(Long.valueOf(userId)))
					.partySize(partySize)
					.requestedAt(requestedAt)
					.updatedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
					.build();

				r.markUpdated(LocalDateTime.now(), ReservationStatus.CANCELLED);
				Reservation saved = reservationRepository.save(r);
				waitingRedisRepository.deleteWaiting(storeId, userId);

				return EntryStatusResponseDto.fromEntity(saved);

			default:
				throw new UnsupportedReservationStatusException(newStatus);
		}
	}


	@Transactional
	public EntryStatusResponseDto processEntryStatusByReservationNumber(Long storeId, String reservationNumber,
		MemberDetails member, ReservationStatus newStatus) {

		User user = getUser(member);
		validateUpdateAuthorization(user, storeId);

		if (reservationNumber == null || reservationNumber.isBlank()) {
			throw new InvalidReservationParameterException("reservationNumber 값이 비어있습니다.");
		}

		// Redis에서 userId 역추적
		String userId = waitingRedisRepository.getUserIdByReservationNumber(storeId, reservationNumber);
		if (userId == null) {
			throw new ReservationDataInconsistencyException(
				String.format("storeId=%d, reservationNumber=%s 에 해당하는 userId를 찾을 수 없습니다.", storeId, reservationNumber)
			);
		}

		String queueKey = RedisKeyUtils.buildWaitingKeyPrefix() + storeId;

		String currStatus = waitingRedisRepository.getWaitingStatus(storeId, userId);
		Double score = redisTemplate.opsForZSet().score(queueKey, userId);
		Integer partySize = waitingRedisRepository.getWaitingPartySize(storeId, userId);


		if (partySize == null || partySize <= 0) {
			throw new InvalidReservationParameterException("partySize가 유효하지 않습니다. (storeId=" + storeId + ", reservationNumber=" + reservationNumber + ")");
		}

		if (currStatus == null) {
			throw new ReservationDataInconsistencyException(
				String.format("storeId=%d, reservationNumber=%s 의 상태값을 찾을 수 없습니다.", storeId, reservationNumber)
			);
		}

		LocalDateTime requestedAt = score != null
			? Instant.ofEpochMilli(score.longValue()).atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime()
			: LocalDateTime.now();
		LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

		switch (newStatus) {
			case CALLING:
				if (!ReservationStatus.WAITING.name().equals(currStatus)) {
					throw new InvalidReservationStatusTransitionException(
						ReservationStatus.valueOf(currStatus), ReservationStatus.CALLING
					);
				}
				waitingRedisRepository.setWaitingStatus(storeId, userId, ReservationStatus.CALLING.name());
				waitingRedisRepository.setWaitingCalledAt(storeId, userId,
					now.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli());

				return EntryStatusResponseDto.builder()
					.reservationNumber(reservationNumber)
					.userId(userId)
					.partySize(partySize)
					.userName(userRepository.getReferenceById(Long.valueOf(userId)).getNickname())
					.createdAt(requestedAt)
					.status("CALLING")
					.updatedAt(now)
					.message("호출되었습니다.")
					.build();

			case CONFIRMED:
				// 1) 기존 대기 중이거나 호출 중일 때: Redis → DB 최초 저장
				if (ReservationStatus.WAITING.name().equals(currStatus) || ReservationStatus.CALLING.name()
					.equals(currStatus)) {

					if (reservationNumber != null) {
						waitingPermitLuaRepository.removeActiveMember(
							userId, String.valueOf(storeId), reservationNumber
						);
					}

					// 새 Reservation 생성 & 저장
					Reservation r = Reservation.builder()
						.reservationNumber(reservationNumber)
						.store(storeRepository.getReferenceById(storeId))
						.user(userRepository.getReferenceById(Long.valueOf(userId)))
						.partySize(partySize)
						.requestedAt(requestedAt)
						.updatedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
						.status(ReservationStatus.valueOf(currStatus))
						.build();

					// 호출 시각 반영
					r.markUpdated(LocalDateTime.now(), ReservationStatus.CONFIRMED);
					Reservation saved = reservationRepository.save(r);
					// Redis 전부 삭제
					waitingRedisRepository.deleteWaiting(storeId, userId);
					return EntryStatusResponseDto.fromEntity(saved);
				} else {
					if (reservationNumber != null) {
						try {
							waitingPermitLuaRepository.removeActiveMember(userId, String.valueOf(storeId),
								reservationNumber);
						} catch (Exception ignore) {
						}
					}

					// 2) 이미 취소(CANCELLED)된 경우: DB 레코드 찾아 바로 CONFIRMED 로 전환
					LocalDateTime start = LocalDate.now().atStartOfDay();
					LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);
					Reservation existing = reservationRepository
						.findFirstByStore_StoreIdAndUserIdAndStatusInAndRequestedAtBetweenOrderByRequestedAtDesc(
							storeId,
							Long.valueOf(userId),
							List.of(ReservationStatus.CANCELLED),
							start,
							end
						).orElseThrow(() -> new ReservationDataInconsistencyException(
							String.format("취소된 예약이 DB에 존재하지 않습니다. (storeId=%d, userId=%s)", storeId, userId)
						));

					existing.markUpdated(LocalDateTime.now(ZoneId.of("Asia/Seoul")), ReservationStatus.CONFIRMED);
					Reservation saved = reservationRepository.save(existing);
					return EntryStatusResponseDto.fromEntity(saved);
				}

			case CANCELLED:
				if (!(ReservationStatus.WAITING.name().equals(currStatus)
					  || ReservationStatus.CALLING.name().equals(currStatus))) {
					throw new InvalidReservationStatusTransitionException(
						ReservationStatus.valueOf(currStatus), ReservationStatus.CANCELLED
					);
				}

				waitingPermitLuaRepository.removeActiveMember(userId, String.valueOf(storeId), reservationNumber);

				Reservation r = Reservation.builder()
					.reservationNumber(reservationNumber)
					.store(storeRepository.getReferenceById(storeId))
					.user(userRepository.getReferenceById(Long.valueOf(userId)))
					.partySize(partySize)
					.requestedAt(requestedAt)
					.updatedAt(now)
					.status(ReservationStatus.valueOf(currStatus))
					.build();

				r.markUpdated(now, ReservationStatus.CANCELLED);
				Reservation saved = reservationRepository.save(r);
				waitingRedisRepository.deleteWaiting(storeId, userId);

				return EntryStatusResponseDto.fromEntity(saved);

			default:
				throw new UnsupportedReservationStatusException(newStatus);
		}
	}


	/**
	 * 공통 메서드
	 */
	// 오늘 날짜 예약 조회
	private List<Reservation> findTodayWaiting(Long storeId) {
		ZoneId zone = ZoneId.of("Asia/Seoul");
		LocalDate today = LocalDate.now(zone);

		return reservationRepository.findAllByStore_StoreIdAndStatusInAndRequestedAtBetween(
			storeId,
			List.of(ReservationStatus.CONFIRMED, ReservationStatus.CANCELLED),
			today.atStartOfDay(zone).toLocalDateTime(),
			today.atTime(LocalTime.MAX)
		);
	}

	// 사용자 인증
	private void validateViewAuthorization(User user, Long storeId) {
		if (!(Role.SUPER_ADMIN.equals(user.getRole())
			  || (Role.MANAGER.equals(user.getRole()) && storeId.equals(user.getStoreId())))) {
			throw new ReservationViewUnauthorizedException();
		}
	}

	private void validateUpdateAuthorization(User user, Long storeId) {
		if (!(Role.SUPER_ADMIN.equals(user.getRole())
			  || (Role.MANAGER.equals(user.getRole()) && storeId.equals(user.getStoreId())))) {
			throw new ReservationUpdateUnauthorizedException();
		}
	}

	private User getUser(MemberDetails memberDetails) {
		if (memberDetails == null) {
			throw new ReservationViewUnauthorizedException();
		}
		return userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
	}
}

