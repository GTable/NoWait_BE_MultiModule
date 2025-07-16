package com.nowait.domaincoreredis.common.util;

public class RedisKeyUtils {

	private static final String KEY_CURRENT = "nowait:store:rank:current";
	private static final String KEY_PREVIOUS = "nowait:store:rank:previous";
	private static final String KEY_NEXT = "nowait:store:rank:next";

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
}
