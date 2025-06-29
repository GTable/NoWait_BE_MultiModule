package com.example.apiuser.store.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.example.apiuser.store.dto.StoreReadDto;
import com.example.apiuser.store.dto.StoreReadResponse;

public interface StoreService {

	StoreReadResponse getAllStores();

	public StoreReadResponse getAllStoresByPage(Pageable pageable);

	StoreReadDto getStoreByStoreId(Long storeId);

	List<StoreReadDto> searchStoresByName(String name);

}
