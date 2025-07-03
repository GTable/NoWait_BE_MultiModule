package com.example.domaintoken.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.domaintoken.entity.Token;

public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByUserId(Long userId);
}
