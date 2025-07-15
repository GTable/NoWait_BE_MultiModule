package com.nowait.applicationadmin.storepayment.service;

import com.nowait.applicationadmin.storepayment.dto.StorePaymentCreateRequest;
import com.nowait.applicationadmin.storepayment.dto.StorePaymentCreateResponse;
import com.nowait.applicationadmin.storepayment.dto.StorePaymentReadDto;
import com.nowait.applicationadmin.storepayment.dto.StorePaymentUpdateRequest;
import com.nowait.domaincorerdb.user.entity.MemberDetails;

public interface StorePaymentService {

	StorePaymentCreateResponse createStorePayment(StorePaymentCreateRequest request, MemberDetails memberDetails);
	StorePaymentReadDto getStorePaymentByStoreId(MemberDetails memberDetails);
	StorePaymentReadDto updateStorePayment(StorePaymentUpdateRequest request, MemberDetails memberDetails);
}
