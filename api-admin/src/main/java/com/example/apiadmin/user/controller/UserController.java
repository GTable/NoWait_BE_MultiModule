package com.example.apiadmin.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.apiadmin.user.dto.ManagerLoginRequestDto;
import com.example.apiadmin.user.dto.ManagerSignupRequestDto;
import com.example.apiadmin.user.serivce.UserService;
import com.nowaiting.common.api.ApiUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "User API", description = "사용자 API")
@RestController
@RequestMapping("admin/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;

    // 관리자 회원가입
    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "관리자 회원가입 / 실제로는 화면 구현X")
    @ApiResponse(responseCode = "201", description = "회원가입")
    public ResponseEntity<?> signup(@RequestBody @Valid ManagerSignupRequestDto managerSignupRequestDto) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                ApiUtils.success(
                    userService.signup(managerSignupRequestDto)
                )
            );
    }

    @PostMapping("/login")
    @Operation(summary = "관리자 로그인", description = "관리자 로그인")
    @ApiResponse(responseCode = "200", description = "관리자 로그인")
    public ResponseEntity<?> login(@RequestBody @Valid ManagerLoginRequestDto managerLoginRequestDto) {
        return ResponseEntity
            .ok()
            .body(
                ApiUtils.success(
                    userService.login(managerLoginRequestDto)
                )
            );
    }
}
