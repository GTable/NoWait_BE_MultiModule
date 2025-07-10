package com.nowait.applicationadmin.storePayment.dto;

import java.time.LocalDateTime;

import com.nowait.domaincorerdb.storePayment.entity.StorePayment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class StorePaymentReadDto {
	private Long paymentMethodId;
	private Long storeId;
	private String tossUrl;
	private String kakaoPayUrl;
	private String naverPayUrl;
	private LocalDateTime createdAt;

	public static StorePaymentReadDto fromEntity(StorePayment storePayment) {
		return StorePaymentReadDto.builder()
			.paymentMethodId(storePayment.getPaymentMethodId())
			.storeId(storePayment.getStoreId())
			.tossUrl(storePayment.getTossUrl())
			.kakaoPayUrl(storePayment.getKakaoPayUrl())
			.naverPayUrl(storePayment.getNaverPayUrl())
			.createdAt(storePayment.getCreatedAt())
			.build();
	}
}
