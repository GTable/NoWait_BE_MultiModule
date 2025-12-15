package com.nowait.applicationuser.oauth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class KakaoAppLoginResponse{
	private String accessToken;
	private String refreshToken;
	private Long userId;
	private String email;
	private String nickName;
	private String profileImage;
	private boolean phoneEntered;
	private boolean marketingAgree;
	private boolean isNewUser;
}
