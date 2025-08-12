package com.nowait.applicationadmin.reservation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import com.nowait.domaincoreredis.reservation.repository.WaitingRedisRepository;

public class WaitingRedisRepositoryTest {
	@Mock
	private StringRedisTemplate template;
	@Mock private ZSetOperations<String, String> zOps;
	@Mock private HashOperations<String, Object, Object> hOps;

	@InjectMocks
	private WaitingRedisRepository repo;

	@BeforeEach
	void init() {
		MockitoAnnotations.openMocks(this);
		given(template.opsForZSet()).willReturn(zOps);
		given(template.opsForHash()).willReturn(hOps);
	}

	@Test
	@DisplayName("getAllWaitingWithScore : null-safe")
	void get_all_waiting_with_score_null_safe() {
		// given
		given(zOps.rangeWithScores("waiting:100", 0, -1)).willReturn(null);

		// when
		var res = repo.getAllWaitingWithScore(100L);

		// then
		assertThat(res).isEmpty();
	}

	@Test
	@DisplayName("getWaitingStatus : null-safe")
	void get_waiting_status_null_safe() {
		// given
		given(hOps.get("waiting:status:100", "u")).willReturn(null);

		// when
		var v = repo.getWaitingStatus(100L, "u");

		// then
		assertThat(v).isNull();
	}

	@Test
	@DisplayName("deleteWaiting : 모든 관련 키 삭제")
	void delete_waiting_remove_all_keys() {
		// given (nothing)

		// when
		repo.deleteWaiting(100L, "u");

		// then
		then(hOps).should().delete("waiting:status:100", "u");
		then(zOps).should().remove("waiting:100", "u");
		then(hOps).should().delete("waiting:party:100", "u");
		then(hOps).should().delete("reservation:number:100", "u");
		then(hOps).should().delete("waiting:calledAt:100", "u");
	}
}
