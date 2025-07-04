package com.nowait.applicationadmin.config.security;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import com.nowait.applicationadmin.security.jwt.JwtAuthorizationFilter;
import com.nowait.applicationadmin.security.jwt.JwtUtil;
import com.nowait.applicationadmin.security.service.CustomUserDetailService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity // security 활성화 어노테이션
@RequiredArgsConstructor
public class SecurityConfig {
	private final JwtUtil jwtUtil;
	private final CustomUserDetailService customUserDetailService;
	private final CorsConfigurationSource corsConfigurationSource;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.cors(cors -> cors.configurationSource(corsConfigurationSource))
			// CSRF 방어 기능 비활성화 (jwt 토큰을 사용할 것이기에 필요없음)
			.csrf(AbstractHttpConfigurer::disable)
			// 시큐리티 폼 로그인 비활성화
			.formLogin(AbstractHttpConfigurer::disable)
			// HTTP Basic 인증 비활성화
			.httpBasic(AbstractHttpConfigurer::disable)
			// 세션 사용하지 않음
			.sessionManagement(session ->
				session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/api/refresh-token", // refresh token (토큰 갱신)
					"/orders/**",
					"/admin/users/**",
					"/swagger-ui/**",
					"/v3/api-docs/**",
					"/v3/api-docs.json",
					"/api-docs/**",
					"/swagger-resources/**",
					"/webjars/**",
					"/demo-ui.html"
				)
				.permitAll()
				.anyRequest().authenticated() // 그외 요청은 허가된 사람만 인가
			)
			// JWTFiler
			.addFilterBefore(new JwtAuthorizationFilter(jwtUtil, customUserDetailService), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	@Bean
	public AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
		authenticationProvider.setUserDetailsService(customUserDetailService);
		authenticationProvider.setPasswordEncoder(passwordEncoder());
		return authenticationProvider;
	}

}
