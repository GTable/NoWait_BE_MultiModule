package com.nowait.applicationuser.menu.repository;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import com.nowait.applicationuser.menu.dto.MenuImageUploadResponse;
import com.nowait.applicationuser.menu.dto.MenuReadDto;
import com.nowait.domaincorerdb.menu.repository.MenuRepository;
import com.nowait.domaincoreredis.common.cache.CacheInvalidationPublisher;
import com.nowait.domaincoreredis.common.cache.DistributedCachedDao;
import com.nowait.domaincoreredis.common.util.RedisKeyUtils;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * 주점별 메뉴 목록 캐시 (key: storeId)
 * 메뉴/메뉴 이미지 변경은 admin-api 가 커밋 후 storeId 로 무효화 메시지를 발행한다
 */
@Component
public class DistributedCachedMenuDao extends DistributedCachedDao<List<MenuReadDto>> {

	private final MenuRepository menuRepository;

	// ttl 은 무효화 메시지 발행 실패 시 옛 값이 남는 최대 시간. 주점 200개라 짧아도 히트율 손해가 작다
	// ttl 을 0s 로 주면 매 조회가 DB 로 간다 (캐시 끄기: 비교 측정, 장애 시 차단)
	public DistributedCachedMenuDao(MenuRepository menuRepository, @Value("${nowait.cache.menu.ttl:5m}") Duration ttl,
		RedisMessageListenerContainer container, CacheInvalidationPublisher publisher, MeterRegistry meterRegistry) {
		super(RedisKeyUtils.buildMenuCacheChannel(), ttl, 1_000, container, publisher, meterRegistry);
		this.menuRepository = menuRepository;
	}

	@Override
	protected List<MenuReadDto> load(String storeId) {
		return menuRepository.findAllWithImagesByStoreId(Long.valueOf(storeId)).entrySet().stream()
			.map(e -> MenuReadDto.fromEntity(e.getKey(),
				e.getValue().stream().map(MenuImageUploadResponse::fromEntity).toList()))
			.toList();
	}
}
