package com.nowait.applicationuser.storepayment.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nowait.applicationuser.storepayment.dto.StorePaymentReadDto;
import com.nowait.domaincorerdb.store.exception.StoreNotFoundException;
import com.nowait.domaincorerdb.storepayment.entity.StorePayment;
import com.nowait.domaincorerdb.storepayment.exception.StorePaymentParamEmptyException;
import com.nowait.domaincorerdb.storepayment.repository.StorePaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StorePaymentServiceImpl implements StorePaymentService {

	private final StorePaymentRepository storePaymentRepository;

	@Override
	@Transactional(readOnly = true)
	public StorePaymentReadDto getStorePaymentByStoreId(Long storeId) {
		if (storeId == null) throw new StorePaymentParamEmptyException();

		StorePayment storePayment = storePaymentRepository.findByStoreId(storeId)
			.orElseThrow(StoreNotFoundException::new);

		return StorePaymentReadDto.fromEntity(storePayment);
	}
}
