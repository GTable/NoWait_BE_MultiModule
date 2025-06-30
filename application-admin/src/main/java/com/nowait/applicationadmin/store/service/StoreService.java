package com.nowait.applicationadmin.store.service;

import com.nowait.applicationadmin.store.dto.StoreCreateRequest;
import com.nowait.applicationadmin.store.dto.StoreCreateResponse;
import com.nowait.applicationadmin.store.dto.StoreReadDto;
import com.nowait.applicationadmin.store.dto.StoreUpdateRequest;

public interface StoreService {

	StoreCreateResponse createStore(StoreCreateRequest request);

	StoreReadDto getStoreByStoreId(Long storeId);

	StoreReadDto updateStore(Long storeId, StoreUpdateRequest request);

	String deleteStore(Long storeId);

	Boolean toggleActive(Long storeId);
}
