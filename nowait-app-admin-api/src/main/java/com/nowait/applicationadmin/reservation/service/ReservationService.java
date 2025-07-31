package com.nowait.applicationadmin.reservation.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
import com.nowait.applicationadmin.reservation.repository.WaitingRedisRepository;
import com.nowait.common.enums.ReservationStatus;
import com.nowait.common.enums.Role;
import com.nowait.domaincorerdb.reservation.entity.Reservation;
import com.nowait.domaincorerdb.reservation.exception.ReservationNotFoundException;
import com.nowait.domaincorerdb.reservation.exception.ReservationUpdateUnauthorizedException;
import com.nowait.domaincorerdb.reservation.exception.ReservationViewUnauthorizedException;
import com.nowait.domaincorerdb.reservation.repository.ReservationRepository;
import com.nowait.domaincorerdb.store.repository.StoreRepository;
import com.nowait.domaincorerdb.user.entity.MemberDetails;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domaincorerdb.user.exception.UserNotFoundException;
import com.nowait.domaincorerdb.user.repository.UserRepository;
import com.nowait.domaincoreredis.common.util.RedisKeyUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationService {

	private final ReservationRepository reservationRepository;
	private final UserRepository userRepository;
	private final WaitingRedisRepository waitingRedisRepository;
	private final StoreRepository storeRepository;
	private final RedisTemplate redisTemplate;

	//TODO 성능 비교를 위해 남겨둔 로직
	@Transactional(readOnly = true)
	public ReservationStatusSummaryDto getReservationListByStoreId(Long storeId, MemberDetails memberDetails) {
		User user = userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
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
		User user = userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
		Reservation reservation = reservationRepository.findById(reservationId)
			.orElseThrow(ReservationNotFoundException::new);
		if (!Role.SUPER_ADMIN.equals(user.getRole()) && !user.getStoreId()
			.equals(reservation.getStore().getStoreId())) {
			throw new ReservationUpdateUnauthorizedException();
		}
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
		Map<String, String> nicknameMap = userRepository.findAllById(userIdLongs).stream()
			.collect(Collectors.toMap(
				u -> u.getId().toString(),
				User::getNickname
			));

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

			result.add(
				WaitingUserResponse.fromRedis(reservationId, userId, partySize, userName, createdAt, calledAt, status,
					score));
		}

		return result;
	}

	// 완료 or 취소 처리된 대기 리스트 조회
	@Transactional(readOnly = true)
	public List<WaitingUserResponse> getCompletedWaitingUserDetails(Long storeId, MemberDetails memberDetails) {
		authorize(storeId, memberDetails);
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

		authorize(storeId, member);

		String queueKey = RedisKeyUtils.buildWaitingKeyPrefix() + storeId;

		// Redis에서 상태·score·partySize·calledAt 조회
		String reservationNumber = waitingRedisRepository.getReservationId(storeId, userId);
		String currStatus = waitingRedisRepository.getWaitingStatus(storeId, userId);
		Double score = redisTemplate.opsForZSet().score(queueKey, userId);
		Integer partySize = waitingRedisRepository.getWaitingPartySize(storeId, userId);

		LocalDateTime requestedAt = score != null
			? Instant.ofEpochMilli(score.longValue()).atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime()
			: LocalDateTime.now();
		LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

		switch (newStatus) {
			case CALLING:
				if (!ReservationStatus.WAITING.name().equals(currStatus)) {
					throw new IllegalStateException("WAITING 상태에서만 CALLING 가능합니다.");
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

					// 새 Reservation 생성 & 저장
					Reservation r = Reservation.builder()
						.reservationNumber(reservationNumber)
						.store(storeRepository.getReferenceById(storeId))
						.user(userRepository.getReferenceById(Long.valueOf(userId)))
						.partySize(partySize)
						.requestedAt(requestedAt)
						.updatedAt(LocalDateTime.now())
						.build();

					// 호출 시각 반영
					r.markUpdated(LocalDateTime.now(), ReservationStatus.CONFIRMED);
					Reservation saved = reservationRepository.save(r);
					// Redis 전부 삭제
					waitingRedisRepository.deleteWaiting(storeId, userId);
					return EntryStatusResponseDto.fromEntity(saved);
				} else {
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
						).orElseThrow(() -> new IllegalStateException("취소된 예약이 없습니다."));

					existing.markUpdated(LocalDateTime.now(), ReservationStatus.CONFIRMED);
					Reservation saved = reservationRepository.save(existing);
					return EntryStatusResponseDto.fromEntity(saved);
				}

			case CANCELLED:
				if (!(ReservationStatus.WAITING.name().equals(currStatus)
					  || ReservationStatus.CALLING.name().equals(currStatus))) {
					throw new IllegalStateException("WAITING/CALLING 상태에서만 취소 가능합니다.");
				}

				Reservation r = Reservation.builder()
					.reservationNumber(reservationNumber)
					.store(storeRepository.getReferenceById(storeId))
					.user(userRepository.getReferenceById(Long.valueOf(userId)))
					.partySize(partySize)
					.requestedAt(requestedAt)
					.updatedAt(LocalDateTime.now())
					.build();

				r.markUpdated(LocalDateTime.now(), ReservationStatus.CANCELLED);
				Reservation saved = reservationRepository.save(r);
				waitingRedisRepository.deleteWaiting(storeId, userId);

				return EntryStatusResponseDto.fromEntity(saved);

			default:
				throw new IllegalArgumentException("지원하지 않는 상태: " + newStatus);
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
	private User authorize(Long storeId, MemberDetails member) {
		User u = userRepository.findById(member.getId())
			.orElseThrow(UserNotFoundException::new);
		if (!Role.SUPER_ADMIN.equals(u.getRole()) && !storeId.equals(u.getStoreId())) {
			throw new ReservationViewUnauthorizedException();
		}
		return u;
	}
}

