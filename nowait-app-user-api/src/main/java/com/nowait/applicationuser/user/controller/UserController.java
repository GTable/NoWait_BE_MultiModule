package com.nowait.applicationuser.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nowait.applicationuser.token.dto.AuthenticationResponse;
import com.nowait.applicationuser.token.dto.NewAccessTokenResponse;
import com.nowait.applicationuser.user.dto.UserUpdateRequest;
import com.nowait.applicationuser.user.service.UserService;
import com.nowait.common.api.ApiUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@PutMapping("/optional-info")
	public ResponseEntity<?> putOptional(
		@Valid @RequestBody UserUpdateRequest req) {

		NewAccessTokenResponse newAccessTokenResponse = userService.putOptional(req.phoneNumber(),
			Boolean.TRUE.equals(req.consent()), req.accessToken());

		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				ApiUtils.success(
					newAccessTokenResponse
				)
			);
	}
}
