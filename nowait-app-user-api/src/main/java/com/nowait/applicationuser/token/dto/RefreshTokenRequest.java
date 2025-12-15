package com.nowait.applicationuser.token.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

// TODO : 사용하는 DTO인지 확인 필요
@Getter
@NoArgsConstructor
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh token은 필수입니다.")
    private String refreshToken;
}
