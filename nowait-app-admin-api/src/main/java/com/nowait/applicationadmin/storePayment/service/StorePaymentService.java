package com.nowait.applicationadmin.storePayment.service;

import com.nowait.applicationadmin.storePayment.dto.StorePaymentCreateRequest;
import com.nowait.applicationadmin.storePayment.dto.StorePaymentCreateResponse;
import com.nowait.applicationadmin.storePayment.dto.StorePaymentReadDto;
import com.nowait.applicationadmin.storePayment.dto.StorePaymentUpdateRequest;
import com.nowait.domaincorerdb.user.entity.MemberDetails;

public interface StorePaymentService {

	StorePaymentCreateResponse createStorePayment(StorePaymentCreateRequest request, MemberDetails memberDetails);
	StorePaymentReadDto getStorePaymentByStoreId(MemberDetails memberDetails);
	StorePaymentReadDto updateStorePayment(StorePaymentUpdateRequest request, MemberDetails memberDetails);
}
