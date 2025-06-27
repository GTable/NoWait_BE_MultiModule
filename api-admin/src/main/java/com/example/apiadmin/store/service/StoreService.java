package com.example.apiadmin.store.service;

import com.example.apiadmin.store.dto.StoreCreateRequest;
import com.example.apiadmin.store.dto.StoreCreateResponse;
import com.example.apiadmin.store.dto.StoreReadDto;
import com.example.apiadmin.store.dto.StoreReadResponse;
import com.example.apiadmin.store.dto.StoreUpdateRequest;

public interface StoreService {

	StoreCreateResponse createStore(StoreCreateRequest request);

	StoreReadResponse getAllStores();

	StoreReadDto getStoreByStoreId(Long storeId);

	StoreReadDto updateStore(Long storeId, StoreUpdateRequest request);

	String deleteStore(Long storeId);

}
