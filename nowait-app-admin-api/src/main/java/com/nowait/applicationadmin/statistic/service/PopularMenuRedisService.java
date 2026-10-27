package com.nowait.applicationadmin.statistic.service;

import java.util.List;

import com.nowait.applicationadmin.statistic.dto.PopularMenuDto;
import com.nowait.domaincorerdb.user.entity.MemberDetails;

public interface PopularMenuRedisService {
	List<PopularMenuDto> getTodayTop5(MemberDetails memberDetails);
}
