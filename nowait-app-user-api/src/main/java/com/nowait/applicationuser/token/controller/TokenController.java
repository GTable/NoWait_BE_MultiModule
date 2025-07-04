package com.nowait.applicationuser.token.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nowait.applicationuser.security.jwt.JwtUtil;
import com.nowait.applicationuser.token.dto.AuthenticationResponse;
import com.nowait.applicationuser.token.dto.RefreshTokenRequest;
import com.nowait.applicationuser.token.service.TokenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/refresh-token")
@Slf4j
public class TokenController {
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    @Value("${jwt.access-token-expiration-ms}")
    private long accessTokenExpiration;
    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpiration;

    @PostMapping
    @Operation(summary = "리프레시 토큰으로 새로운 액세스 토큰 발급", description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급합니다.")
    @ApiResponse(responseCode = "200", description = "새로운 액세스 토큰 발급 성공")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request){
        String refreshToken = request.getRefreshToken();

        // 리프레시 토큰 검증
        Long userId = jwtUtil.getUserId(refreshToken);
        String role = jwtUtil.getRole(refreshToken);

        long currentAccessTokenExpiration = accessTokenExpiration;
        if (role.equals("SUPER_ADMIN")) {
            currentAccessTokenExpiration = 7L * 24 * 60 * 60 * 1000L; // 7일
        }

        // 리프레시 토큰 유효성 검증
        if (tokenService.validateToken(refreshToken, userId)){
            // 유효한 토큰이라면, 새로운 accessToken, refreshToken 생성
            String newAccessToken = jwtUtil.createAccessToken("accessToken", userId, role, currentAccessTokenExpiration);
            String newRefreshToken = jwtUtil.createRefreshToken("refreshToken", userId, refreshTokenExpiration);

            // DB에 새로운 refreshToken으로 교체
            tokenService.updateRefreshToken(userId, refreshToken, newRefreshToken);

            AuthenticationResponse authenticationResponse = new AuthenticationResponse(newAccessToken, refreshToken);
            return ResponseEntity.ok().body(authenticationResponse);

        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired refresh token");
    }
}
