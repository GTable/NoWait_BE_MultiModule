package com.nowait.applicationadmin.statistic.service.impl;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationadmin.statistic.dto.StoreRankingDto;
import com.nowait.applicationadmin.statistic.service.RankingService;
import com.nowait.common.enums.Role;
import com.nowait.domainadminrdb.statistic.dto.StoreInfo;
import com.nowait.domainadminrdb.statistic.exception.StatisticViewUnauthorizedException;
import com.nowait.domainadminrdb.statistic.repository.StatisticCustomRepository;
import com.nowait.domaincorerdb.user.entity.MemberDetails;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domaincorerdb.user.exception.UserNotFoundException;
import com.nowait.domaincorerdb.user.repository.UserRepository;
import com.nowait.domaincoreredis.rank.dto.RankingEntry;
import com.nowait.domaincoreredis.rank.service.RankingQueryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

	private final StatisticCustomRepository statisticCustomRepository;
	private final UserRepository userRepository;
	private final RankingQueryService rankingQuery;


	@Override
	@Transactional(readOnly = true)
	public List<StoreRankingDto> getStatisticsRankings(MemberDetails memberDetails) {
		User user = userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
		Long userStoreId = user.getStoreId();

		if (!Role.SUPER_ADMIN.equals(user.getRole()) && !user.getStoreId().equals(userStoreId)) {
			throw new StatisticViewUnauthorizedException();
		}

		// 1) Redis에서 Top4+내주점: storeId, totalSales, currentRank, delta
		List<RankingEntry> entries = rankingQuery.getRankings(userStoreId, 5);

		// 2) DB에서 store 정보 가져오기
		List<Long> storeIds = entries.stream()
			.map(RankingEntry::getStoreId)
			.toList();

		List<StoreInfo> infos = statisticCustomRepository.findStoreInfoByIds(storeIds);

		// StoreInfo를 storeId로 매핑
		Map<Long, StoreInfo> infoMap = infos.stream()
			.collect(Collectors.toMap(StoreInfo::getStoreId, Function.identity()));

		// 3) 매핑 → 최종 DTO
		return entries.stream()
			.map(e -> {
				StoreInfo info = infoMap.get(e.getStoreId());
				return new StoreRankingDto(
					e.getStoreId(),
					info.getStoreName(),
					info.getDepartmentId(),
					info.getDepartmentName(),
					e.getTotalSales(),
					e.getCurrentRank(),
					e.getDelta()
				);
			})
			.collect(Collectors.toList());
	}
}
