package com.nowait.applicationuser.store.service;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;

import com.nowait.applicationuser.store.dto.StoreDepartmentReadResponse;
import com.nowait.applicationuser.store.dto.StoreDetailReadResponse;
import com.nowait.applicationuser.store.dto.StorePageReadDto;
import com.nowait.applicationuser.store.dto.StorePageReadResponse;
import com.nowait.applicationuser.store.dto.StoreWaitingInfo;
import com.nowait.domainuserrdb.oauth.dto.CustomOAuth2User;

public interface StoreService {

	StoreDepartmentReadResponse getAllStoresByPageAndDeparments(Pageable pageable);

	StoreDetailReadResponse getStoreByStoreId(Long storeId, CustomOAuth2User customOAuth2User);

	List<StorePageReadResponse> searchByKeywordNative(String name);

	List<StoreWaitingInfo> getStoresByWaitingCount(boolean desc);

	List<StorePageReadDto> getAllStoresByPageAndDeparments(List<Long> storeIds, Set<Long> bookmarkedSet);
}
