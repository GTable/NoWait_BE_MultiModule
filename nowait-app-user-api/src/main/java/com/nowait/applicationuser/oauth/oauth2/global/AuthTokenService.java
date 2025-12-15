package com.nowait.applicationuser.oauth.oauth2.global;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationuser.security.jwt.JwtUtil;
import com.nowait.domaincorerdb.token.entity.Token;
import com.nowait.domaincorerdb.token.repository.TokenRepository;
import com.nowait.domaincorerdb.user.entity.User;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

	private final JwtUtil jwtUtil;
	private final TokenRepository tokenRepository;

	@Transactional
	public TokenResult issueTokens(User user) {
		Long userId = user.getId();
		String role = user.getRole().name();

		String accessToken = jwtUtil.createAccessToken(
			"accessToken",
			userId,
			role,
			Boolean.TRUE.equals(user.getPhoneEntered()),
			Boolean.TRUE.equals(user.getIsMarketingAgree()),
			60 * 60 * 2000L
		); // 2시간

		String refreshToken = jwtUtil.createRefreshToken(
			"refreshToken",
			userId,
			30L * 24 * 60 * 60 * 1000L
		); // 30일

		// refreshToken을 DB에 저장 or update
		Optional<Token> tokenOptional = tokenRepository.findByUserId(user.getId());
		if (tokenOptional.isPresent()) {
			Token token = tokenOptional.get();
			token.updateRefreshToken(refreshToken, LocalDateTime.now().plusDays(30));
		} else {
			Token token = Token.toEntity(user, refreshToken, LocalDateTime.now().plusDays(30));
			tokenRepository.save(token);
		}

		return new TokenResult(accessToken, refreshToken);
	}

	public static class TokenResult {
		private final String accessToken;
		private final String refreshToken;

		public TokenResult(String accessToken, String refreshToken) {
			this.accessToken = accessToken;
			this.refreshToken = refreshToken;
		}

		public String getAccessToken() { return accessToken; }
		public String getRefreshToken() { return refreshToken; }
	}
}
