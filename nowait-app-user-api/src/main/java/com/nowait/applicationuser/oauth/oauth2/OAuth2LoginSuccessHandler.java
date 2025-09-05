package com.nowait.applicationuser.oauth.oauth2;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationuser.security.jwt.JwtUtil;
import com.nowait.domaincorerdb.token.entity.Token;
import com.nowait.domaincorerdb.token.repository.TokenRepository;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domainuserrdb.oauth.dto.CustomOAuth2User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 카카오 로그인 성공 시, 콜백 핸들러
// 1. JWT 토큰 발급
// - 이때, JWT payload는 보안상 최소한의 정보(userId, role)만 담겠다
// 2. refreshToken만 DB에 저장
// 3. JSON 응답으로, accessToken과 refreshToken 을 반환해준다.
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
	private final JwtUtil jwtUtil;
	private final TokenRepository tokenRepository;

	@Override
	@Transactional
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException {

		CustomOAuth2User customUserDetails = (CustomOAuth2User)authentication.getPrincipal();
		User user = customUserDetails.getUser();
		Long userId = customUserDetails.getUserId();
		String role = authentication.getAuthorities().iterator().next().getAuthority();

		// JWT 발급
		String accessToken = jwtUtil.createAccessToken("accessToken", userId, role,
			Boolean.TRUE.equals(user.getPhoneEntered()),  Boolean.TRUE.equals(user.getIsMarketingAgree()),60 * 60 * 1000L); // 1시간
		String refreshToken = jwtUtil.createRefreshToken("refreshToken", userId, 30L * 24 * 60 * 60 * 1000L); // 30일

		// 1. refreshToken을 DB에 저장 or update
		Optional<Token> tokenOptional = tokenRepository.findByUserId(user.getId());
		if (tokenOptional.isPresent()) {
			Token token = tokenOptional.get();
			token.updateRefreshToken(refreshToken, LocalDateTime.now().plusDays(30));
		} else {
			Token token = Token.toEntity(user, refreshToken, LocalDateTime.now().plusDays(30));
			tokenRepository.save(token);
		}

		// 2. refreshToken을 HttpOnly 쿠키로 설정 (ResponseCookie로)
		ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
			.httpOnly(true)
			.secure(false) // 운영환경에서는 true
			.path("/")
			.maxAge(30L * 24 * 60 * 60) // 30일 (초 단위)
			.sameSite("Lax")
			.build();

		// 기존 방식 대신 ResponseCookie.toString()을 헤더로 추가
		response.setHeader("Set-Cookie", refreshTokenCookie.toString());

		// 3. 프론트엔드로 리다이렉트 (accessToken만 쿼리로 전달)
		String targetUrl = "https://nowait-user.vercel.app/login/success?accessToken=" + accessToken;
		response.sendRedirect(targetUrl);
	}
}
