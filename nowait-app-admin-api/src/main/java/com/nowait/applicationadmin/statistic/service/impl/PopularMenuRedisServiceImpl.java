package com.nowait.applicationadmin.statistic.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import com.nowait.applicationadmin.statistic.dto.PopularMenuDto;
import com.nowait.applicationadmin.statistic.service.PopularMenuRedisService;
import com.nowait.common.enums.Role;
import com.nowait.domainadminrdb.statistic.exception.StatisticViewUnauthorizedException;
import com.nowait.domaincorerdb.menu.entity.Menu;
import com.nowait.domaincorerdb.menu.repository.MenuRepository;
import com.nowait.domaincorerdb.user.entity.MemberDetails;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domaincorerdb.user.exception.UserNotFoundException;
import com.nowait.domaincorerdb.user.repository.UserRepository;
import com.nowait.domaincoreredis.rank.service.MenuCounterService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PopularMenuRedisServiceImpl implements PopularMenuRedisService {

	private final MenuCounterService menuCounterService;
	private final MenuRepository menuRepository;
	private final UserRepository userRepository;

	@Override
	public List<PopularMenuDto> getTodayTop5(MemberDetails memberDetails) {

		User user = userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
		Long storeId = user.getStoreId();

		if (!Role.SUPER_ADMIN.equals(user.getRole()) && !user.getStoreId().equals(storeId)) {
			throw new StatisticViewUnauthorizedException();
		}

		Set<ZSetOperations.TypedTuple<String>> tuples = menuCounterService.getTopMenus(storeId, 5);

		List<Long> menuIds = tuples.stream()
			.map(tuple -> Long.parseLong(tuple.getValue()))
			.toList();

		Map<Long, String> menuIdToNameMap = menuRepository.findAllById(menuIds)
			.stream()
			.collect(Collectors.toMap(
				Menu::getId,
				Menu::getName
			));


		return tuples.stream()
			.map(tuple -> {
				Long menuId = Long.parseLong(tuple.getValue());
				String menuName = menuIdToNameMap.getOrDefault(menuId, "Unknown Menu");

				return new PopularMenuDto(menuId, menuName, tuple.getScore().longValue());
			})
			.toList();
	}
}
