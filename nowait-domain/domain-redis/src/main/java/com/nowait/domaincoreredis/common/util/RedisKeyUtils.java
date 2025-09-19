package com.nowait.domaincoreredis.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class RedisKeyUtils {

	// Store rank keys
	private static final String KEY_CURRENT = "nowait:store:rank:current";
	private static final String KEY_PREVIOUS = "nowait:store:rank:previous";
	private static final String KEY_NEXT = "nowait:store:rank:next";

	// Menu rank keys
	private static final String KEY_FMT = "popular:%d:%s";
	private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyyMMdd");

	// Waiting keys
	private static final String WAITING_KEY_PREFIX = "waiting:";
	private static final String WAITING_PARTYSIZE_KEY_PREFIX = "waiting:party:";
	private static final String WAITING_STATUS_KEY_PREFIX = "waiting:status:";

	// Waiting User keys
	public static String buildUserHoldingKey(String userId) {
		return "waiting:user:{" + userId + "}:holding"; // ZSET(member=token, score=expireEpochMs)
	}
	public static String buildUserActiveKey(String userId) {
		return "waiting:user:{" + userId + "}:active";  // SET(member="storeId:reservationId")
	}


	private RedisKeyUtils() {
		throw new UnsupportedOperationException("유틸리티 서비스는 인스턴스화 할 수 없습니다.");
	}

	public static String buildCurrentKey() {
		return KEY_CURRENT;
	}

	public static String buildPreviousKey() {
		return KEY_PREVIOUS;
	}

	public static String buildNextKey() {
		return KEY_NEXT;
	}

	public static String buildMenuKey() {
		return KEY_FMT;
	}

	public static DateTimeFormatter buildMenuDateKey() {
		return DTF;
	}

	public static String buildWaitingKeyPrefix() {
		return WAITING_KEY_PREFIX;
	}

	public static String buildWaitingPartySizeKeyPrefix() {
		return WAITING_PARTYSIZE_KEY_PREFIX;
	}

	public static String buildWaitingStatusKeyPrefix() {
		return WAITING_STATUS_KEY_PREFIX;
	}

	// Waiting Reservation Number key
	public static String buildReservationSeqKey(Long storeId) {
		return String.format("reservation:seq:%d", storeId);
	}

	public static String buildReservationNumberKey(Long storeId) {
		return String.format("reservation:number:%d", storeId);
	}

	public static String buildReservationUserKey(Long storeId) {
		return String.format("reservation:user:%d", storeId);
	}

	/**
	 * 대기 호출 시각(hash)에 사용할 키 접두사
	 */
	public static String buildWaitingCalledAtKeyPrefix() {
		return "waiting:calledAt:";
	}

	public static Date expireAtNext03() {
		ZoneId zone = ZoneId.of("Asia/Seoul");
		LocalDateTime now = LocalDateTime.now(zone);
		LocalDateTime next03 = now.toLocalDate().plusDays(1).atTime(3, 0);
		Instant instant = next03.atZone(zone).toInstant();

		return Date.from(instant);
	}
}
