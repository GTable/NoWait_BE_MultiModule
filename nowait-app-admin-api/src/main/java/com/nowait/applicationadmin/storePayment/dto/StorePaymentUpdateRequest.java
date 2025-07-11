package com.nowait.applicationadmin.storePayment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StorePaymentUpdateRequest {
	private String tossUrl;
	private String kakaoPayUrl;
	private String naverPayUrl;
}
