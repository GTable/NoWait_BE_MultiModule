package com.nowait.applicationuser.storepayment.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nowait.applicationuser.storepayment.dto.StorePaymentReadDto;
import com.nowait.domaincorerdb.store.entity.Store;
import com.nowait.domaincorerdb.store.exception.StoreNotFoundException;
import com.nowait.domaincorerdb.store.repository.StoreRepository;
import com.nowait.domaincorerdb.storepayment.exception.StorePaymentParamEmptyException;
import com.nowait.domaincorerdb.storepayment.repository.StorePaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StorePaymentServiceImpl implements StorePaymentService {

	private final StorePaymentRepository storePaymentRepository;
	private final StoreRepository storeRepository;

	@Override
	@Transactional(readOnly = true)
	public Optional<StorePaymentReadDto> getStorePaymentByStoreId(String publicCode) {
		if (publicCode == null) throw new StorePaymentParamEmptyException();
		Store store = storeRepository.findByPublicCodeAndDeletedFalse(publicCode)
			.orElseThrow(StoreNotFoundException::new);
		return storePaymentRepository.findByStoreId(store.getStoreId())
			.map(StorePaymentReadDto::fromEntity);
	}
}
