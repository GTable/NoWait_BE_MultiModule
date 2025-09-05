package com.nowait.applicationuser.user.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationuser.security.jwt.JwtUtil;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domaincorerdb.user.exception.UserNotFoundException;
import com.nowait.domaincorerdb.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final JwtUtil jwtUtil;

	@Transactional
	public String putOptional(Long userId, String phoneNumber, boolean consent) {

		User user = userRepository.findById(userId)
			.orElseThrow(UserNotFoundException::new);

		if (userRepository.existsByPhoneNumber(phoneNumber)) {
			throw new IllegalArgumentException("Phone number already in use");
		}

		user.setPhoneNumberAndMarkEntered(phoneNumber, LocalDateTime.now());
		user.setIsMarketingAgree(consent, LocalDateTime.now());

		String role = "ROLE_" + user.getRole().name();

		return jwtUtil.createAccessToken("accessToken", user.getId(), role,
			Boolean.TRUE.equals(user.getPhoneEntered()),
			Boolean.TRUE.equals(user.getIsMarketingAgree()),
			60 * 60 * 1000L);
	}
}
