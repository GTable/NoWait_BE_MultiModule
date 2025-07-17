package com.nowait.domaincoreredis.common.util;

import java.time.format.DateTimeFormatter;

public class RedisKeyUtils {

	// Store rank keys
	private static final String KEY_CURRENT = "nowait:store:rank:current";
	private static final String KEY_PREVIOUS = "nowait:store:rank:previous";
	private static final String KEY_NEXT = "nowait:store:rank:next";

	// Menu rank keys
	private static final String KEY_FMT = "popular:%d:%s";
	private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyyMMdd");


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

	public static String buildMenuKey() { return KEY_FMT; }

	public static DateTimeFormatter buildMenuDateKey() { return DTF; }
}
