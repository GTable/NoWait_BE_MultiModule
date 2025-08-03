package com.nowait.applicationuser.aws.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nowait.common.api.ApiUtils;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class healthCheckController {
	@GetMapping("/health-check")
	public ResponseEntity<?> healthCheck() {
		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				ApiUtils
					.success(
						"정상 작동 중입니다."
					)
			);
	}
}
