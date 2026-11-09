package com.nowait.applicationadmin.reservation.scheduler;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.nowait.common.enums.ReservationStatus;
import com.nowait.domaincorerdb.reservation.entity.Reservation;
import com.nowait.domaincorerdb.reservation.repository.ReservationRepository;
import com.nowait.domaincoreredis.reservation.repository.WaitingRedisRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * DB 를 원본으로 Redis 대기열의 빠진 항목을 채운다. Redis 재시작, Slave 승격 뒤 유실, 부분 유실을 같은 경로로 복구한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WaitingRedisRecoveryScheduler {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private final ReservationRepository reservationRepository;
	private final WaitingRedisRepository waitingRedisRepository;

	// ponytail: 30초마다 당일 진행 중 예약을 전부 훑는다. 행이 많아지면 (status, requested_at) 인덱스를 추가한다
	@Scheduled(fixedDelay = 30_000)
	public void restoreMissingWaitings() {
		List<Reservation> active = reservationRepository.findActiveSince(
			List.of(ReservationStatus.WAITING, ReservationStatus.CALLING), lastResetTime());

		long restored = active.stream().filter(this::isV1Reservation).filter(this::restore).count();
		if (restored > 0) {
			log.warn("Redis 대기열 복구 - 복구 {}건 / 진행 중 예약 {}건", restored, active.size());
		}
	}

	// 대기열 키는 매일 03시에 만료되므로 그 이후 등록분만 진행 중으로 본다
	private LocalDateTime lastResetTime() {
		LocalDateTime now = LocalDateTime.now(SEOUL);
		LocalDateTime reset = now.toLocalDate().atTime(3, 0);
		return now.isBefore(reset) ? reset.minusDays(1) : reset;
	}

	// v2(yyyyMMdd-storeId-seq)는 Redis 구조가 달라 이 복구 대상이 아니다
	private boolean isV1Reservation(Reservation r) {
		return r.getReservationNumber() != null && r.getReservationNumber().startsWith(r.getStore().getStoreId() + "-");
	}

	private boolean restore(Reservation r) {
		Long calledAt = r.getStatus() == ReservationStatus.CALLING ? toEpochMilli(r.getUpdatedAt()) : null;
		return waitingRedisRepository.restoreWaiting(r.getStore().getStoreId(), r.getUser().getId(),
			toEpochMilli(r.getRequestedAt()), r.getPartySize(), r.getStatus().name(), r.getReservationNumber(), calledAt);
	}

	private long toEpochMilli(LocalDateTime time) {
		return time.atZone(SEOUL).toInstant().toEpochMilli();
	}
}
