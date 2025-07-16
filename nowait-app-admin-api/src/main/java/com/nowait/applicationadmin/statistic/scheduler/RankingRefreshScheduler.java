package com.nowait.applicationadmin.statistic.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.nowait.domainadminrdb.statistic.dto.StoreSales;
import com.nowait.domainadminrdb.statistic.repository.StatisticCustomRepository;
import com.nowait.domaincoreredis.common.util.RedisKeyUtils;
import com.nowait.domaincoreredis.rank.repository.RankingQueryRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableScheduling
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

	@Scheduled(cron = "0 */5 * * * *") // 매 5분마다 실행
	public void refresh() {
		log.info("RankingRefreshScheduler.refresh() called at {}", LocalDateTime.now());

		String nextKey = RedisKeyUtils.buildNextKey();
		String currentKey = RedisKeyUtils.buildCurrentKey();
		String previousKey = RedisKeyUtils.buildPreviousKey();

		// 1) 다음 스냅샷 키 초기화
		redis.delete(nextKey);

		// 2) DB에서 매출 합계 가져와 ZADD
		List<StoreSales> salesList = statisticCustomRepository.findTotalSales();

		salesList.forEach(s ->
			rankingQueryRepository.addToRanking(nextKey, s.getStoreId(), s.getTotalSales())
		);

		// 3) 현재 스냅샷 키를 이전 스냅샷 키로 이동
		if (redis.hasKey(currentKey)) {
			redis.rename(currentKey, previousKey);
		}
		if (redis.hasKey(nextKey)) {
			redis.rename(nextKey, currentKey);
		}
	}
}
