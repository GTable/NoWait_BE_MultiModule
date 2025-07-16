package com.nowait.applicationadmin.statistic.service;

import java.util.List;

import com.nowait.applicationadmin.statistic.dto.StoreRankingDto;
import com.nowait.domaincorerdb.user.entity.MemberDetails;

public interface RankingService {
	List<StoreRankingDto> getStatisticsRankings(MemberDetails memberDetails);
}
