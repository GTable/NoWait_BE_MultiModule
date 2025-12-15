package com.nowait.domainuserrdb.oauth.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.nowait.domaincorerdb.user.entity.User;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class CustomOAuth2User implements OAuth2User {
	private final User user;
	private final boolean newUser;

	@Override
	public Map<String, Object> getAttributes() {
		return null;
	}

	// 사용자가 가지는 권한 설정
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		Collection<GrantedAuthority> authorities = new ArrayList<>();

		authorities.add(new GrantedAuthority() {
			@Override
			public String getAuthority() {
				return user.getRole().getName(); // 유저의 권한 리턴
			}
		});

		return authorities;
	}

	// JWT 인증 시 (항상 기존 유저 취급)
	public CustomOAuth2User(User user) {
		this.user = user;
		this.newUser = false;
	}

	@Override
	public String getName() {
		return user.getEmail();
	}

	public User getUser() {
		return user;
	}

	public Long getUserId() {
		return user.getId();
	}

	public String getNickname() {
		return user.getNickname();
	}

}
