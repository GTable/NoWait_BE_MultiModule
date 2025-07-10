package com.nowait.applicationadmin.storePayment.dto;

import com.nowait.domaincorerdb.storePayment.entity.StorePayment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StorePaymentCreateRequest {

	private String tossUrl;
	private String kakaoPayUrl;
	private String naverPayUrl;

	public StorePayment toEntity(Long storeId) {
		return StorePayment.builder()
			.storeId(storeId)
			.tossUrl(tossUrl)
			.kakaoPayUrl(kakaoPayUrl)
			.naverPayUrl(naverPayUrl)
			.build();
	}
}
