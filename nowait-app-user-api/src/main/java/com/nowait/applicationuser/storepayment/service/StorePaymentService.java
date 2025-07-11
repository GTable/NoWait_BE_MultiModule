package com.nowait.applicationuser.storepayment.service;

import com.nowait.applicationuser.storepayment.dto.StorePaymentReadDto;

public interface StorePaymentService {
	StorePaymentReadDto getStorePaymentByStoreId(Long storeId);
}
