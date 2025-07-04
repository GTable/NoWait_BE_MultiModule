package com.nowait.applicationuser.security.jwt;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.nowait.common.enums.Role;
import com.nowait.common.enums.SocialType;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domainuserrdb.oauth.dto.CustomOAuth2User;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// JWT 검증 필터
// 1. 헤더에서 accessToken 추출, 2. 토큰 검증, 3. 유효하면 사용자정보를 SecurityContextHolder에 세팅
// 그러면, 이후 컨트롤러에서 @AuthenticationPrincipal에서 저장했던 사용자 정보를 꺼내쓸 수 있음
@RequiredArgsConstructor
@Slf4j
public class JwtAuthorizationFilter extends OncePerRequestFilter {
	private final JwtUtil jwtUtil;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		String header = request.getHeader("Authorization");

		// 인증헤더 Bearer가 없다면, 다음 필터로 넘김
		if (header == null || !header.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);

			log.debug("JwtAuthorizationFilter: Authorization 헤더가 없거나 Bearer 토큰 형식이 아님. JWT 인증 필터를 건너뜁니다. [header={}] ", header);
			return;
		}

		log.info("header :: {}, header.substring(7) :: {}", header, header.substring(7));
		String accessToken = header.substring(7);

		// 토큰 만료 여부 확인, 만료 시 다음 필터로 넘기지 않음
		try {
			jwtUtil.isExpired(accessToken);
		} catch (ExpiredJwtException e) {

			// response status code + msg
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.getWriter().print("access token expired");

			log.warn("JwtAuthorizationFilter: 만료된 AccessToken 입니다. 토큰 인증 거부, URI: {}", request.getRequestURI());
			return;
		}

		// 토큰이 accessToken 종류인지 확인
		String tokenCategory = jwtUtil.getTokenCategory(accessToken);

		if (!tokenCategory.equals("accessToken")) {
			//response status code
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.getWriter().print("invalid access token");

			log.warn("JwtAuthorizationFilter: 잘못된 토큰 유형(accessToken 아님)으로 인증 요청. URI: {}, tokenCategory: {}", request.getRequestURI(), tokenCategory);
			return;
		}

		// userId와 role 값 추출
		Long userId = jwtUtil.getUserId(accessToken);
		String roleString = jwtUtil.getRole(accessToken);

		if (userId == null || roleString == null) {
			log.warn("JwtAuthorizationFilter: JWT에서 userId 또는 role 추출 실패. 토큰: {}", accessToken);
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.getWriter().print("invalid token");
			return;
		}

		User user = User.createUserWithId(userId, "sampleEmail", "sampleNickname", "sampleProfileImg"
			, SocialType.KAKAO, Role.fromString(roleString));

		CustomOAuth2User customOAuth2User = new CustomOAuth2User(user);

		// 스프링 시큐리티 인증 토큰 생성
		UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
			customOAuth2User, null, customOAuth2User.getAuthorities());

		// 생성한 인증 정보를 SecurityContext에 설정
		SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

		log.info("JwtAuthorizationFilter: 인증 성공. userId={}, role={}, URI={}", userId, roleString, request.getRequestURI());

		filterChain.doFilter(request, response);

	}

}
