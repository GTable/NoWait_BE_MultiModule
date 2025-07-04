package com.nowait.applicationadmin.security.jwt;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.nowait.applicationadmin.security.service.CustomUserDetailService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthorizationFilter extends OncePerRequestFilter {
	private final JwtUtil jwtUtil;
	private final CustomUserDetailService userDetailsService;

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		try {
			String token = extractTokenFromRequest(request);
			if (token != null) {
				// 만료 체크
				if (jwtUtil.isExpired(token)) {
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					response.getWriter().print("access token expired");
					return;
				}
				// 토큰 category 체크(불필요하면 생략)
				String tokenCategory = jwtUtil.getTokenCategory(token);
				if (!"accessToken".equals(tokenCategory)) {
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					response.getWriter().print("invalid access token");
					return;
				}
				// userId 추출 → UserDetails 조회
				Long userId = jwtUtil.getUserId(token);
				var userDetails = userDetailsService.loadUserById(userId);
				// 인증 객체 생성 및 컨텍스트에 설정
				UsernamePasswordAuthenticationToken authenticationToken =
					new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
				SecurityContextHolder.getContext().setAuthentication(authenticationToken);
			}
		} catch (Exception ex) {
			log.error("JWT filter error: {}", ex.getMessage());
		} finally {
			filterChain.doFilter(request, response);
		}
	}

	private String extractTokenFromRequest(HttpServletRequest request) {
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith("Bearer ")) {
			return header.substring(7);
		}
		return null;
	}
}
