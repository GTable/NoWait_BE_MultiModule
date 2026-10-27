package com.nowait.applicationuser.oauth.dto;

import com.nowait.domaincorerdb.user.entity.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OAuthUserResult {
	private final User user;
	private final boolean newUser;
}
