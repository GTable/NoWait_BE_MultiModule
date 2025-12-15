package com.nowait.applicationuser.oauth.oauth2;

import java.io.IOException;

import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationuser.token.dto.AuthenticationResponse;
import com.nowait.applicationuser.token.service.AuthTokenService;
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
	private final AuthTokenService authTokenService;

	@Override
	@Transactional
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException {

		CustomOAuth2User customUserDetails = (CustomOAuth2User)authentication.getPrincipal();
		User user = customUserDetails.getUser();

		AuthenticationResponse authenticationResponse = authTokenService.issueTokens(user);

		// 2. refreshToken을 HttpOnly 쿠키로 설정 (ResponseCookie로)
		ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", authenticationResponse.getRefreshToken())
			.httpOnly(true)
			.secure(false) // 운영환경에서는 true
			.path("/")
			.maxAge(30L * 24 * 60 * 60) // 30일 (초 단위)
			.sameSite("Lax")
			.build();

		// 기존 방식 대신 ResponseCookie.toString()을 헤더로 추가
		response.setHeader("Set-Cookie", refreshTokenCookie.toString());

		// 3. 프론트엔드로 리다이렉트 (accessToken만 쿼리로 전달)
		String targetUrl = "https://app.nowait.co.kr/login/success?accessToken=" + authenticationResponse.getAccessToken();
		response.sendRedirect(targetUrl);
	}
}
