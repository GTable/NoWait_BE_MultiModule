package com.nowait.applicationuser.reservation.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MyWaitingStoreInfo {
	private final Long storeId;
	private final String storeName;
	private final String departmentName;
	private final String location;
}
