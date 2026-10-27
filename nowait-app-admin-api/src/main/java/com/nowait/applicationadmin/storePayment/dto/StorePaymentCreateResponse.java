package com.nowait.applicationadmin.storepayment.dto;

import java.time.LocalDateTime;

import com.nowait.domaincorerdb.storepayment.entity.StorePayment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class StorePaymentCreateResponse {

	private Long paymentMethodId;
	private Long storeId;
	private String tossUrl;
	private String kakaoPayUrl;
	private String naverPayUrl;
	private String accountNumber;
	private LocalDateTime createdAt;

	public static StorePaymentCreateResponse fromEntity(StorePayment storePayment) {
		return StorePaymentCreateResponse.builder()
			.paymentMethodId(storePayment.getPaymentMethodId())
			.storeId(storePayment.getStoreId())
			.tossUrl(storePayment.getTossUrl() != null ? storePayment.getTossUrl() : "")
			.kakaoPayUrl(storePayment.getKakaoPayUrl() != null ? storePayment.getKakaoPayUrl() : "")
			.naverPayUrl(storePayment.getNaverPayUrl() != null ? storePayment.getNaverPayUrl() : "")
			.accountNumber(storePayment.getAccountNumber() != null ? storePayment.getAccountNumber() : "")
			.createdAt(storePayment.getCreatedAt())
			.build();
	}
}
