package com.nowait.applicationuser.user.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationuser.security.jwt.JwtUtil;
import com.nowait.applicationuser.token.dto.AuthenticationResponse;
import com.nowait.applicationuser.token.dto.NewAccessTokenResponse;
import com.nowait.applicationuser.token.service.TokenService;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domaincorerdb.user.exception.UserNotFoundException;
import com.nowait.domaincorerdb.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final TokenService tokenService;
	private final JwtUtil jwtUtil;

	@Transactional
	public NewAccessTokenResponse putOptional(String phoneNumber, boolean consent, String accessToken) {

		Long userId = jwtUtil.getUserId(accessToken);;
		String role = jwtUtil.getRole(accessToken);
		AuthenticationResponse authenticationResponse;

		User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

		if (userRepository.existsByPhoneNumberAndIdNot(phoneNumber, userId)) {
			throw new IllegalArgumentException("이미 사용 중인 휴대폰 번호입니다.");
		}

		user.setPhoneNumberAndMarkEntered(phoneNumber, LocalDateTime.now());
		user.setIsMarketingAgree(consent, LocalDateTime.now());

		String newAccessToken = jwtUtil.createAccessToken(
			"accessToken",
			userId,
			role,
			Boolean.TRUE.equals(user.getPhoneEntered()),
			Boolean.TRUE.equals(user.getIsMarketingAgree()),
			60 * 60 * 1000L
		);

		NewAccessTokenResponse newAccessTokenResponse = new NewAccessTokenResponse(newAccessToken);

		return newAccessTokenResponse;
	}
}
