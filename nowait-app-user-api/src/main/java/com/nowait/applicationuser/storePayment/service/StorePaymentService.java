package com.nowait.applicationuser.storePayment.service;

import com.nowait.applicationuser.storePayment.dto.StorePaymentReadDto;

public interface StorePaymentService {
	StorePaymentReadDto getStorePaymentByStoreId(Long storeId);
}
