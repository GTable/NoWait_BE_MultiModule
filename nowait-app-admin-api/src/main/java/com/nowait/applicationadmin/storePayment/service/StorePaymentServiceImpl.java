package com.nowait.applicationadmin.storepayment.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationadmin.storepayment.dto.StorePaymentCreateRequest;
import com.nowait.applicationadmin.storepayment.dto.StorePaymentCreateResponse;
import com.nowait.applicationadmin.storepayment.dto.StorePaymentReadDto;
import com.nowait.applicationadmin.storepayment.dto.StorePaymentUpdateRequest;
import com.nowait.common.enums.Role;
import com.nowait.domaincorerdb.storepayment.entity.StorePayment;
import com.nowait.domaincorerdb.storepayment.exception.StorePaymentAlreadyExistsException;
import com.nowait.domaincorerdb.storepayment.exception.StorePaymentCreationUnauthorizedException;
import com.nowait.domaincorerdb.storepayment.exception.StorePaymentDeleteUnauthorizedException;
import com.nowait.domaincorerdb.storepayment.exception.StorePaymentNotFoundException;
import com.nowait.domaincorerdb.storepayment.exception.StorePaymentParamEmptyException;
import com.nowait.domaincorerdb.storepayment.exception.StorePaymentUpdateUnauthorizedException;
import com.nowait.domaincorerdb.storepayment.exception.StorePaymentViewUnauthorizedException;
import com.nowait.domaincorerdb.storepayment.repository.StorePaymentRepository;
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
		if (request == null) throw new StorePaymentParamEmptyException();

		User user = getUser(memberDetails);
		Long storeId = user.getStoreId();
		if (storePaymentRepository.findByStoreId(storeId).isPresent()) {
			throw new StorePaymentAlreadyExistsException();
		}
		validateCreationAuthorization(user, storeId);

		StorePayment toSave = request.toEntity(storeId);
		StorePayment saved = storePaymentRepository.save(toSave);

		return StorePaymentCreateResponse.fromEntity(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<StorePaymentReadDto> getStorePaymentByStoreId(MemberDetails memberDetails) {
		if (memberDetails == null) throw new StorePaymentParamEmptyException();

		User user = getUser(memberDetails);
		Long storeId = user.getStoreId();
		validateViewAuthorization(user, storeId);

		return storePaymentRepository.findByStoreId(storeId)
			.map(StorePaymentReadDto::fromEntity);
	}

	@Override
	@Transactional
	public StorePaymentReadDto updateStorePayment(StorePaymentUpdateRequest request, MemberDetails memberDetails) {
		if (request == null) throw new StorePaymentParamEmptyException();

		User user = getUser(memberDetails);
		Long storeId = user.getStoreId();
		validateUpdateAuthorization(user, storeId);
		StorePayment storePayment = storePaymentRepository.findByStoreId(storeId)
			.orElseThrow(StorePaymentNotFoundException::new);

		storePayment.updatePaymentMethodUrl(
			request.getTossUrl(),
			request.getKakaoPayUrl(),
			request.getNaverPayUrl(),
			request.getAccountNumber()
		);
		storePaymentRepository.save(storePayment);

		return StorePaymentReadDto.fromEntity(storePayment);
	}

	private User getUser(MemberDetails memberDetails) {
		return userRepository.findById(memberDetails.getId()).orElseThrow(UserNotFoundException::new);
	}

	private void validateViewAuthorization(User user, Long storeId) {
		if (!(Role.SUPER_ADMIN.equals(user.getRole()) || user.getStoreId().equals(storeId))) {
			throw new StorePaymentViewUnauthorizedException();
		}
	}

	private void validateCreationAuthorization(User user, Long storeId) {
		if (!(Role.SUPER_ADMIN.equals(user.getRole()) || user.getStoreId().equals(storeId))) {
			throw new StorePaymentCreationUnauthorizedException();
		}
	}

	private void validateUpdateAuthorization(User user, Long storeId) {
		if (!(Role.SUPER_ADMIN.equals(user.getRole()) || user.getStoreId().equals(storeId))) {
			throw new StorePaymentUpdateUnauthorizedException();
		}
	}

	private void validateDeleteAuthorization(User user, Long storeId) {
		if (!(Role.SUPER_ADMIN.equals(user.getRole()) || user.getStoreId().equals(storeId))) {
			throw new StorePaymentDeleteUnauthorizedException();
		}
	}
}
