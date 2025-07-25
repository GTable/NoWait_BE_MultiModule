package com.nowait.applicationuser.store.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.nowait.applicationuser.store.dto.StoreDepartmentReadResponse;
import com.nowait.applicationuser.store.dto.StorePageReadDto;
import com.nowait.applicationuser.store.dto.StoreReadDto;
import com.nowait.applicationuser.store.dto.StoreReadResponse;
import com.nowait.applicationuser.store.dto.StoreWaitingInfo;

public interface StoreService {

	StoreDepartmentReadResponse getAllStoresByPageAndDeparments(Pageable pageable);

	StorePageReadDto getStoreByStoreId(Long storeId);

	List<StorePageReadDto> searchByKeywordNative(String name);

	List<StoreWaitingInfo> getStoresByWaitingCount(boolean desc);

	List<StorePageReadDto> getAllStoresByPageAndDeparments(List<Long> storeIds);
}
