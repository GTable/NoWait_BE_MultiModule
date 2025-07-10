package com.nowait.applicationadmin.storePayment.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationadmin.storePayment.dto.StorePaymentCreateRequest;
import com.nowait.applicationadmin.storePayment.dto.StorePaymentCreateResponse;
import com.nowait.applicationadmin.storePayment.dto.StorePaymentReadDto;
import com.nowait.applicationadmin.storePayment.dto.StorePaymentUpdateRequest;
import com.nowait.common.enums.Role;
import com.nowait.domaincorerdb.store.exception.StoreNotFoundException;
import com.nowait.domaincorerdb.store.exception.StoreParamEmptyException;
import com.nowait.domaincorerdb.store.exception.StoreViewUnauthorizedException;
import com.nowait.domaincorerdb.storePayment.entity.StorePayment;
import com.nowait.domaincorerdb.storePayment.repository.StorePaymentRepository;
import com.nowait.domaincorerdb.user.entity.MemberDetails;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domaincorerdb.user.exception.UserNotFoundException;
import com.nowait.domaincorerdb.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StorePaymentServiceImpl implements StorePaymentService {

	private final StorePaymentRepository storePaymentRepository;
	private final UserRepository userRepository;

	@Override
	@Transactional
	public StorePaymentCreateResponse createStorePayment(StorePaymentCreateRequest request, MemberDetails memberDetails) {
		// if (request == null) throw new StoreParamEmptyException();
		User user = userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
		Long storeId = user.getStoreId();
		if (!Role.SUPER_ADMIN.equals(user.getRole()) && !user.getStoreId().equals(storeId)) {
			throw new StoreViewUnauthorizedException();
		}
		StorePayment toSave = request.toEntity(storeId);
		StorePayment saved = storePaymentRepository.save(toSave);

		return StorePaymentCreateResponse.fromEntity(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public StorePaymentReadDto getStorePaymentByStoreId(MemberDetails memberDetails) {
		// if (storeId == null) throw new StoreParamEmptyException();
		User user = userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
		Long storeId = user.getStoreId();
		if (!Role.SUPER_ADMIN.equals(user.getRole()) && !user.getStoreId().equals(storeId)) {
			throw new StoreViewUnauthorizedException();
		}
		StorePayment storePayment = storePaymentRepository.findByStoreId(storeId)
			.orElseThrow(StoreNotFoundException::new);

		return StorePaymentReadDto.fromEntity(storePayment);
	}

	@Override
	@Transactional
	public StorePaymentReadDto updateStorePayment(StorePaymentUpdateRequest request, MemberDetails memberDetails) {
		if (request == null) throw new StoreParamEmptyException();
		User user = userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
		Long storeId = user.getStoreId();
		if (!Role.SUPER_ADMIN.equals(user.getRole()) && !user.getStoreId().equals(storeId)) {
			throw new StoreViewUnauthorizedException();
		}
		StorePayment storePayment = storePaymentRepository.findByStoreId(storeId)
			.orElseThrow(StoreNotFoundException::new);

		storePayment.updatePaymentMethodUrl(
			request.getTossUrl(),
			request.getKakaoPayUrl(),
			request.getNaverPayUrl()
		);
		storePaymentRepository.save(storePayment);

		return StorePaymentReadDto.fromEntity(storePayment);
	}
}
