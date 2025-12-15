package com.nowait.applicationuser.user.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationuser.security.jwt.JwtUtil;
import com.nowait.applicationuser.token.dto.AuthenticationResponse;
import com.nowait.applicationuser.token.service.AuthTokenService;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domaincorerdb.user.exception.UserNotFoundException;
import com.nowait.domaincorerdb.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final AuthTokenService authTokenService;
	private final JwtUtil jwtUtil;

	@Transactional
	public AuthenticationResponse putOptional(String phoneNumber, boolean consent, String accessToken) {

		Long userId = jwtUtil.getUserId(accessToken);

		User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

		if (userRepository.existsByPhoneNumberAndIdNot(phoneNumber, userId)) {
			throw new IllegalArgumentException("이미 사용 중인 휴대폰 번호입니다.");
		}

		user.setPhoneNumberAndMarkEntered(phoneNumber, LocalDateTime.now());
		user.setIsMarketingAgree(consent, LocalDateTime.now());

		return authTokenService.issueTokens(user);
	}
}
