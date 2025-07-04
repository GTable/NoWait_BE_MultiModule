package com.nowait.applicationuser.token.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationuser.security.exception.RefreshTokenNotFoundException;
import com.nowait.applicationuser.security.exception.TokenBadRequestException;
import com.nowait.applicationuser.security.jwt.JwtUtil;
import com.nowait.domaincorerdb.token.entity.Token;
import com.nowait.domaincorerdb.token.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {
    private final TokenRepository tokenRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public boolean validateToken(String token, Long userId){
        // DB에서 해당 userId와 일치하는 리프레시토큰을 찾는다.
        Optional<Token> savedToken = tokenRepository.findByUserId(userId);
        
        // DB에서 userId에 대응되는 리프레시토큰 없으면, 유효하지 않음
        if (savedToken.isEmpty()){
            log.debug("DB에 현재 userId에 대응되는 리프레시 토큰이 없습니다");
            return false;
        }
        
        // 리프레시 토큰이 DB에 저장된 토큰과 일치하는지 확인
        if (!savedToken.get().getRefreshToken().equals(token)){
            log.debug("DB에 저장된 리프레시 토큰와 현재 전달받은 리프레시 토큰 일치하지 않습니다");
            return false;
        }
        
        // 리프레시 토큰의 만료여부 확인
        if(jwtUtil.isExpired(token)){
            log.debug("리프레시 토큰이 만료된 토큰입니다");
            return false; // 만료된 토큰은 유효하지 않음
        }

        log.info("리프레시 토큰이 유효한 토큰입니다");
        return true; // 모든 조건 만족 시, 유효한 토큰
    }

    @Transactional
    public void updateRefreshToken(Long userId, String oldRefreshToken, String newRefreshToken){
        Token token = tokenRepository.findByUserId(userId)
                .orElseThrow(RefreshTokenNotFoundException::new); // 404

        if (!token.getRefreshToken().equals(oldRefreshToken)){
            throw new TokenBadRequestException(); // 400
        }

        // 기존 토큰 삭제 및 새 토큰 저장
        tokenRepository.delete(token);
        Token newToken = Token.toEntity(token.getUser(), newRefreshToken, LocalDateTime.now().plusDays(30));
        tokenRepository.save(newToken);
    }
}
