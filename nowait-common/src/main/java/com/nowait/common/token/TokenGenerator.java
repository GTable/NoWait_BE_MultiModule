package com.nowait.common.token;

import java.security.SecureRandom;

public final class TokenGenerator {
	private static final SecureRandom RNG = new SecureRandom();
	private static final char[] ALPHABET =
		"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

	// 12~16 권장
	public static String base62(int len) {
		char[] out = new char[len];
		for (int i = 0; i < len; i++) out[i] = ALPHABET[RNG.nextInt(ALPHABET.length)];
		return new String(out);
	}
	private TokenGenerator() {}
}
