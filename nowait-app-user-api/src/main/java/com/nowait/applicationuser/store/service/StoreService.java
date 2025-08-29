package com.nowait.applicationuser.store.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Pageable;

import com.nowait.applicationuser.store.dto.StoreDepartmentReadResponse;
import com.nowait.applicationuser.store.dto.StoreDetailReadResponse;
import com.nowait.applicationuser.store.dto.StorePageReadResponse;
import com.nowait.applicationuser.store.dto.StoreSearchResponse;
import com.nowait.applicationuser.store.dto.StoreWaitingInfo;
import com.nowait.domaincorerdb.user.entity.MemberDetails;
import com.nowait.domainuserrdb.oauth.dto.CustomOAuth2User;

public interface StoreService {

	StoreDepartmentReadResponse getAllStoresByPageAndDeparments(Pageable pageable, CustomOAuth2User customOAuth2User);

	StoreDetailReadResponse getStoreByPublicCode(String publicCode, CustomOAuth2User customOAuth2User);

	List<StoreSearchResponse> searchByKeywordNative(String name);

	List<StoreWaitingInfo> getStoresByWaitingCount(boolean desc);

	List<StorePageReadResponse> getAllStoresByPageAndDeparments(List<Long> storeIds, Map<Long, Long> bookmarkMap);
}
