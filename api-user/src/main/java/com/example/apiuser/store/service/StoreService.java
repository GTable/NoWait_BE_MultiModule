package com.example.apiuser.store.service;

import java.util.List;

import com.example.apiuser.store.dto.StoreReadDto;
import com.example.apiuser.store.dto.StoreReadResponse;

public interface StoreService {

	StoreReadResponse getAllStores();

	StoreReadDto getStoreByStoreId(Long storeId);

	List<StoreReadDto> searchStoresByName(String name);

}
