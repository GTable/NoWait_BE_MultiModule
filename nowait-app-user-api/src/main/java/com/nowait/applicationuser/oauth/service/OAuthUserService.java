package com.nowait.applicationuser.oauth.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationuser.oauth.dto.OAuth2Response;
import com.nowait.applicationuser.oauth.dto.OAuthUserResult;
import com.nowait.common.enums.Role;
import com.nowait.common.enums.SocialType;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domaincorerdb.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OAuthUserService {
	private final UserRepository userRepository;

	@Transactional
	public OAuthUserResult loadOrCreateUser(OAuth2Response oAuth2Response) {
		return userRepository.findByEmail(oAuth2Response.getEmail())
			.map(user -> new OAuthUserResult(user, false))   // 기존 유저
			.orElseGet(() -> {
				User created = createUser(oAuth2Response);
				return new OAuthUserResult(created, true);  // 신규 유저
			});
	}

	private User createUser(OAuth2Response oAuth2Response) {
		User user = User.builder()
			.email(oAuth2Response.getEmail())
			.phoneNumber("")
			.nickname(oAuth2Response.getNickName())
			.profileImage(oAuth2Response.getProfileImage())
			.socialType(SocialType.KAKAO)
			.role(Role.USER) // 일반 유저 설정
			.storeId(0L)
			.phoneEntered(false)
			.isMarketingAgree(false)
			.createdAt(LocalDateTime.now())
			.updatedAt(LocalDateTime.now())
			.build();

		return userRepository.save(user);
	}
}
