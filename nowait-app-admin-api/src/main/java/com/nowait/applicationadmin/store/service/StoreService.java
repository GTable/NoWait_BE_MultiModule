package com.nowait.applicationadmin.store.service;

import com.nowait.applicationadmin.store.dto.StoreCreateRequest;
import com.nowait.applicationadmin.store.dto.StoreCreateResponse;
import com.nowait.applicationadmin.store.dto.StoreDetailReadResponse;
import com.nowait.applicationadmin.store.dto.StoreReadDto;
import com.nowait.applicationadmin.store.dto.StoreUpdateRequest;
import com.nowait.domaincorerdb.user.entity.MemberDetails;

public interface StoreService {

	StoreCreateResponse createStore(StoreCreateRequest request);

	StoreDetailReadResponse getStoreByStoreId(Long storeId, MemberDetails memberDetails);

	StoreReadDto updateStore(Long storeId, StoreUpdateRequest request, MemberDetails memberDetails);

	String deleteStore(Long storeId, MemberDetails memberDetails);

	Boolean toggleActive(Long storeId);
}
