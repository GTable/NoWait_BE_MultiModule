package com.nowait.applicationuser.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nowait.applicationuser.token.dto.AuthenticationResponse;
import com.nowait.applicationuser.user.dto.UserUpdateRequest;
import com.nowait.applicationuser.user.service.UserService;
import com.nowait.common.api.ApiUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/users/me")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	// TODO : Pathch로 변경 고려 및 토큰 전달 방식 변경 고려 (SecurityContext)
	@PutMapping("/optional")
	public ResponseEntity<?> putOptional(
		@Valid @RequestBody UserUpdateRequest request) {

		AuthenticationResponse authenticationResponse = userService.putOptional(request.phoneNumber(),
			Boolean.TRUE.equals(request.consent()), request.accessToken());

		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				ApiUtils.success(
					authenticationResponse
				)
			);
	}
}
