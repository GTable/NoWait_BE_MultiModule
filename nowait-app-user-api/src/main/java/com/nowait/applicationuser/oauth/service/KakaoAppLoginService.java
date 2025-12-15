package com.nowait.applicationuser.oauth.service;

import java.time.Instant;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.nowait.applicationuser.oauth.dto.KakaoAppLoginRequest;
import com.nowait.applicationuser.oauth.dto.KakaoAppLoginResponse;
import com.nowait.applicationuser.token.dto.AuthenticationResponse;
import com.nowait.applicationuser.token.service.AuthTokenService;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domainuserrdb.oauth.dto.CustomOAuth2User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KakaoAppLoginService {

	private final CustomOAuth2UserService customOAuth2UserService;
	private final ClientRegistrationRepository clientRegistrationRepository;
	private final AuthTokenService authTokenService;

	public KakaoAppLoginResponse login(KakaoAppLoginRequest request) {
		String kakaoAccessTokenValue = request.getKakaoAccessToken();

		if (kakaoAccessTokenValue == null || kakaoAccessTokenValue.isEmpty()) {
			throw new OAuth2AuthenticationException("Kakao Access Token is missing.");
		}

		ClientRegistration kakaoRegistration = clientRegistrationRepository.findByRegistrationId("kakao");

		if (kakaoRegistration == null) {
			throw new OAuth2AuthenticationException("Kakao Client Registration not found.");
		}

		OAuth2AccessToken kakaoAccessToken = new OAuth2AccessToken(
			OAuth2AccessToken.TokenType.BEARER,
			kakaoAccessTokenValue,
			Instant.now(),
			null
		);

		OAuth2UserRequest userRequest = new OAuth2UserRequest(
			kakaoRegistration,
			kakaoAccessToken
		);

		OAuth2User oAuth2User = customOAuth2UserService.loadUser(userRequest);
		CustomOAuth2User customUser = (CustomOAuth2User) oAuth2User;
		User user = customUser.getUser();

		AuthenticationResponse authenticationResponse = authTokenService.issueTokens(user);

		return KakaoAppLoginResponse.builder()
			.accessToken(authenticationResponse.getAccessToken())
			.refreshToken(authenticationResponse.getRefreshToken())
			.userId(user.getId())
			.email(user.getEmail())
			.nickName(user.getNickname())
			.profileImage(user.getProfileImage())
			.phoneEntered(user.getPhoneEntered())
			.marketingAgree(user.getIsMarketingAgree())
			.isNewUser(customUser.isNewUser())
			.build();
	}
}
