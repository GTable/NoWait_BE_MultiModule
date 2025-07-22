package com.nowait.applicationadmin.statistic.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.nowait.domainadminrdb.statistic.dto.StoreSales;
import com.nowait.domainadminrdb.statistic.repository.StatisticCustomRepository;
import com.nowait.domaincoreredis.common.util.RedisKeyUtils;
import com.nowait.domaincoreredis.rank.repository.RankingQueryRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Component
@RequiredArgsConstructor
@Slf4j
public class RankingRefreshScheduler {

	private final StatisticCustomRepository statisticCustomRepository;
	private final RankingQueryRepository rankingQueryRepository;
	private final RedisTemplate<String, String> redis;

	@PostConstruct
	public void init() {
		// 초기화 작업
		refresh();
	}

	@Scheduled(cron = "*/30 * * * * *") // 매 5분마다 실행
	public void refresh() {
		log.info("RankingRefreshScheduler.refresh() called at {}", LocalDateTime.now());

		try {
			doRefresh();
		} catch (Exception e) {
			log.error("랭킹 데이터 갱신 중 오류 발생", e);
			// 예외 발생 시 알림 또는 로깅 처리
		}
	}

	private void doRefresh() {
		String nextKey = RedisKeyUtils.buildNextKey();
		String currentKey = RedisKeyUtils.buildCurrentKey();
		String previousKey = RedisKeyUtils.buildPreviousKey();

		// 1) 다음 스냅샷 키 초기화
		redis.delete(nextKey);

		// 2) DB에서 매출 합계 가져와 ZADD
		List<StoreSales> salesList = statisticCustomRepository.findTotalSales();

		if (salesList.isEmpty()) {
			log.warn("매출 데이터가 없습니다. 다음 스냅샷 키를 초기화합니다.");
			redis.delete(currentKey);
			redis.delete(previousKey);

			return;
		}

		salesList.forEach(s ->
			rankingQueryRepository.addToRanking(nextKey, s.getStoreId(), s.getTotalSales())
		);

		// 3) 현재 스냅샷 키를 이전 스냅샷 키로 이동
		rotateKeys(currentKey, previousKey, nextKey);
	}

	private void rotateKeys(String currentKey, String previousKey, String nextKey) {
		try {
			redis.execute((RedisCallback<Object>)connection -> {
				// 현재 스냅샷 키를 이전 스냅샷 키로 이동하고, 다음 스냅샷 키를 현재 스냅샷 키로 이동
				if (redis.hasKey(previousKey)) {
					redis.delete(previousKey);
				}
				// 현재 스냅샷 키를 다음 스냅샷 키로 이동
				if (redis.hasKey(currentKey)) {
					redis.rename(currentKey, previousKey);
				}
				// 다음 스냅샷 키가 존재하면 현재 스냅샷 키로 이동
				if (redis.hasKey(nextKey)) {
					redis.rename(nextKey, currentKey);
				}
				return null;
			});
			log.info("Keys rotated: current -> {}, previous -> {}, next -> {}", currentKey, previousKey, nextKey);
		} catch (Exception e) {
			log.error("Redis 키 교체 중 오류 발생", e);
			throw new RuntimeException("랭킹 데이터 갱신 실패", e);
		}
	}
}
