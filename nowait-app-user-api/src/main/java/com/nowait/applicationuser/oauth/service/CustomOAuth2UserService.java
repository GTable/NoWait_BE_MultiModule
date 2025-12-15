package com.nowait.applicationuser.oauth.service;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.nowait.applicationuser.oauth.dto.KaKaoResponse;
import com.nowait.applicationuser.oauth.dto.OAuth2Response;
import com.nowait.applicationuser.oauth.dto.OAuthUserResult;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domainuserrdb.oauth.dto.CustomOAuth2User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// OAuth2 제공자(카카오)로부터 제공받은 사용자 정보를, 우리 서비스에 맞게 가공, 변환
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
	private final OAuthUserService oAuthUserService;

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oAuth2User = super.loadUser(userRequest);

		log.info("CustomOAuth2UserService :: {}", oAuth2User);
		log.info("oAuthUser.getAttributes :: {}", oAuth2User.getAttributes());

		String registrationId = userRequest.getClientRegistration().getRegistrationId();
		OAuth2Response oAuth2Response = null;

		if (registrationId.equals("kakao")) {
			oAuth2Response = new KaKaoResponse(oAuth2User.getAttributes());
		} else {
			throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 Provider 입니다.");
		}

		// DB에 유저가 있는지 판단
		OAuthUserResult result = oAuthUserService.loadOrCreateUser(oAuth2Response);
		User user = result.getUser();
		boolean newUser = result.isNewUser();

		return new CustomOAuth2User(user, newUser);
	}
}
