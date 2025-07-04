package com.nowait.applicationadmin.user.serivce;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationadmin.security.jwt.JwtUtil;
import com.nowait.applicationadmin.user.dto.ManagerLoginRequestDto;
import com.nowait.applicationadmin.user.dto.ManagerLoginResponseDto;
import com.nowait.applicationadmin.user.dto.ManagerSignupRequestDto;
import com.nowait.applicationadmin.user.dto.ManagerSignupResponseDto;
import com.nowait.domaincorerdb.user.entity.MemberDetails;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domaincorerdb.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationProvider authenticationProvider;
	private final JwtUtil jwtUtil;
	@Value("${jwt.access-token-expiration-ms}")
	private long accessTokenExpiration;

	@Transactional
	public ManagerSignupResponseDto signup(ManagerSignupRequestDto managerSignupRequestDto) {
		validateEmailDuplicated(managerSignupRequestDto);
		validateNickNameDuplicated(managerSignupRequestDto.getNickname());
		User user = managerSignupRequestDto.toEntity();
		user.encodePassword(passwordEncoder);

		return ManagerSignupResponseDto.fromEntity(userRepository.save(user));

	}
	private void validateEmailDuplicated(ManagerSignupRequestDto managerSignupRequestDto) {
		userRepository.findByEmail(managerSignupRequestDto.getEmail()).ifPresent(member -> {
				throw new IllegalArgumentException();
			}
		);
	}
	private void validateNickNameDuplicated(String nickName) {
		userRepository.findByNickname(nickName).ifPresent(member -> {
				throw new IllegalArgumentException();
			}
		);
	}
	@Transactional
	public ManagerLoginResponseDto login(ManagerLoginRequestDto managerLoginRequestDto) {
		Authentication authentication = authenticationProvider.authenticate(
			new UsernamePasswordAuthenticationToken(managerLoginRequestDto.getEmail(), managerLoginRequestDto.getPassword())
		);
		MemberDetails memberDetails = (MemberDetails) authentication.getPrincipal();
		User user = userRepository.getReferenceById(memberDetails.getId());

		long currentAccessTokenExpiration = accessTokenExpiration;
		if (user.getRole() == com.nowait.common.enums.Role.SUPER_ADMIN) {
			currentAccessTokenExpiration = 7L * 24 * 60 * 60 * 1000L; // 7일
		}

		String accessToken = jwtUtil.createAccessToken("accessToken", user.getId(), String.valueOf(user.getRole()), currentAccessTokenExpiration);
		return ManagerLoginResponseDto.fromEntity(user,accessToken);
	}
}
