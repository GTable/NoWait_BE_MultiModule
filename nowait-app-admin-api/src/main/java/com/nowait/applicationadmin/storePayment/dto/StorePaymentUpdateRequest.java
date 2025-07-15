package com.nowait.applicationadmin.storepayment.dto;

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
