package com.nowait.applicationuser.waiting.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GetWaitingSizeResponse {
	private Long storeId;
	private String storeName;
	private String departmentName;
	private Long waitingCount;
}
