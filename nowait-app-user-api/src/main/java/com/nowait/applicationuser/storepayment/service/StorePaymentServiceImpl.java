package com.nowait.applicationuser.storepayment.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nowait.applicationuser.storepayment.dto.StorePaymentReadDto;
import com.nowait.domaincorerdb.storepayment.exception.StorePaymentParamEmptyException;
import com.nowait.domaincorerdb.storepayment.repository.StorePaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StorePaymentServiceImpl implements StorePaymentService {

	private final StorePaymentRepository storePaymentRepository;

	@Override
	@Transactional(readOnly = true)
	public Optional<StorePaymentReadDto> getStorePaymentByStoreId(Long storeId) {
		if (storeId == null) throw new StorePaymentParamEmptyException();

		return storePaymentRepository.findByStoreId(storeId)
			.map(StorePaymentReadDto::fromEntity);
	}
}
