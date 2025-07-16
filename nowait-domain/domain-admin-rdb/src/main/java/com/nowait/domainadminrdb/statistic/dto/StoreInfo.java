package com.nowait.domainadminrdb.statistic.dto;

import lombok.Getter;

@Getter
public class StoreInfo {
	private final Long storeId;
	private final String storeName;
	private final Long departmentId;
	private final String departmentName;

	public StoreInfo(Long storeId, String storeName, Long departmentId, String departmentName) {
		this.storeId = storeId;
		this.storeName = storeName;
		this.departmentId = departmentId;
		this.departmentName = departmentName;
	}
}
