package com.nowait.applicationuser.waiting.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.nowait.applicationuser.waiting.dto.WaitingCancelIdempotencyValue;
import com.nowait.applicationuser.waiting.dto.WaitingIdempotencyValue;
import com.nowait.applicationuser.waiting.redis.WaitingIdempotencyRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

	private final WaitingIdempotencyRepository waitingIdempotencyRepository;

	// 멱등키 검증 메서드
	// TODO : 공통 멱등키 검증 메서드로 리팩토링 필요!!!!!!!!!
	public Optional<WaitingIdempotencyValue> validateIdempotency(HttpServletRequest httpServletRequest) {
		String idempotentKey = httpServletRequest.getHeader("Idempotency-Key");

		return waitingIdempotencyRepository.findByRegisterKey(idempotentKey);
	}

	public WaitingCancelIdempotencyValue validateIdempotencyCancel(HttpServletRequest httpServletRequest) {
		String idempotentKey = httpServletRequest.getHeader("Idempotency-Key");

		return waitingIdempotencyRepository.findByCancelKey(idempotentKey);
	}

	// 멱등키 응답 저장 메서드
	public void saveIdempotencyResponse(String idempotentKey, Object response) {
		if (idempotentKey != null && !idempotentKey.isBlank()) {
			waitingIdempotencyRepository.saveIdempotencyResponse(idempotentKey, response);
		}
	}

	// 멱등키 PROCESSING 상태로 최초 저장
	public void saveIdempotencyKeyInProgress(String idempotentKey) {
		if (idempotentKey != null && !idempotentKey.isBlank()) {
			log.info("Saving idempotency key in progress: {}", idempotentKey);
			waitingIdempotencyRepository.saveIdempotencyInProgress(idempotentKey);
		}
	}

}
