package com.nowait.applicationadmin.user.serivce;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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
import com.nowait.domaincorerdb.token.entity.Token;
import com.nowait.domaincorerdb.token.repository.TokenRepository;
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
	private final TokenRepository tokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationProvider authenticationProvider;
	private final JwtUtil jwtUtil;
	@Value("${jwt.access-token-expiration-ms}")
	private long accessTokenExpiration;

	@Transactional
	public ManagerSignupResponseDto signup(ManagerSignupRequestDto managerSignupRequestDto) {
		validateEmailDuplicated(managerSignupRequestDto);
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
	@Transactional
	public ResponseEntity<ManagerLoginResponseDto> login(ManagerLoginRequestDto managerLoginRequestDto) {
		Authentication authentication = authenticationProvider.authenticate(
			new UsernamePasswordAuthenticationToken(
				managerLoginRequestDto.getEmail(),
				managerLoginRequestDto.getPassword()
			)
		);
		MemberDetails memberDetails = (MemberDetails) authentication.getPrincipal();
		User user = userRepository.getReferenceById(memberDetails.getId());

		long currentAccessTokenExpiration = accessTokenExpiration;
		if (user.getRole() == com.nowait.common.enums.Role.SUPER_ADMIN) {
			currentAccessTokenExpiration = 100L * 24 * 60 * 60 * 1000L; // 100일
		}

		String accessToken = jwtUtil.createAccessToken("accessToken", user.getId(), String.valueOf(user.getRole()), currentAccessTokenExpiration);
		String refreshToken = jwtUtil.createRefreshToken("refreshToken", user.getId(), 30L * 24 * 60 * 60 * 1000L);

		// 기존 토큰 존재 확인
		Optional<Token> tokenOptional = tokenRepository.findByUserId(user.getId());
		if (tokenOptional.isPresent()) {
			Token token = tokenOptional.get();
			token.updateRefreshToken(refreshToken, LocalDateTime.now().plusDays(30L)); // 엔티티에 update 메소드 구현 권장
		} else {
			tokenRepository.save(
				Token.builder()
					.user(user)
					.refreshToken(refreshToken)
					.expiredDate(LocalDateTime.now().plusDays(30L))
					.build()
			);
		}
		ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
			.httpOnly(true)
			.secure(true) // 운영환경에 맞게
			.path("/")
			.maxAge(30L * 24 * 60 * 60)
			.sameSite("Strict")
			.build();

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
			.body(ManagerLoginResponseDto.fromEntity(user, accessToken));

	}

}
