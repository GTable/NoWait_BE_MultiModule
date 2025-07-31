package com.nowait.applicationuser.storepayment.service;

import java.util.Optional;

import com.nowait.applicationuser.storepayment.dto.StorePaymentReadDto;

public interface StorePaymentService {
	Optional<StorePaymentReadDto> getStorePaymentByStoreId(Long storeId);
}
