package com.nowait.token.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nowait.token.entity.Token;

public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByUserId(Long userId);
}
