package com.nowait.applicationuser.store.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.nowait.applicationuser.store.dto.StoreReadDto;
import com.nowait.applicationuser.store.dto.StoreReadResponse;

public interface StoreService {

	StoreReadResponse getAllStores();

	StoreReadResponse getAllStoresByPage(Pageable pageable);

	StoreReadDto getStoreByStoreId(Long storeId);

	List<StoreReadDto> searchStoresByName(String name);

}
