package com.nowait.applicationuser.token.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString(exclude = {"accessToken"}) // 로깅 시 토큰 노출 방지
public class NewAccessTokenResponse {
    @JsonProperty("access_token")
    private final String accessToken;
}
