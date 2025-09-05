package com.nowait.applicationuser.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nowait.applicationuser.token.dto.AuthenticationResponse;
import com.nowait.applicationuser.user.dto.UserUpdateRequest;
import com.nowait.applicationuser.user.service.UserService;
import com.nowait.common.api.ApiUtils;
import com.nowait.domainuserrdb.oauth.dto.CustomOAuth2User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@PutMapping("/optional-info")
	public ResponseEntity<?> putOptional(
		@CookieValue(value = "accessToken", required = false) String accessToken,
		@Valid @RequestBody UserUpdateRequest req) {

		if (accessToken == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("accessToken not found in cookies");
		}

		AuthenticationResponse authenticationResponse = userService.putOptional(accessToken, req.phoneNumber(),
			Boolean.TRUE.equals(req.consent()));

		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				ApiUtils.success(
					authenticationResponse
				)
			);
	}
}
