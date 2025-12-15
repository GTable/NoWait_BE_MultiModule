package com.nowait.applicationuser.oauth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nowait.applicationuser.oauth.dto.KakaoAppLoginRequest;
import com.nowait.applicationuser.oauth.dto.KakaoAppLoginResponse;
import com.nowait.applicationuser.oauth.service.KakaoAppLoginService;
import com.nowait.common.api.ApiUtils;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v2/app/oauth/kakao")
@RequiredArgsConstructor
public class KakaoAppAuthController {

	private final KakaoAppLoginService kakaoAppLoginService;

	@PostMapping("/login")
	public ResponseEntity<?> kakaoAppLogin(@RequestBody KakaoAppLoginRequest request) {

		KakaoAppLoginResponse response = kakaoAppLoginService.login(request);

		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				ApiUtils.success(response)
			);
	}
}
