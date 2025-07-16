package com.nowait.domainadminrdb.statistic.dto;

import lombok.Getter;

@Getter
public class StoreInfo {
	private Long storeId;
	private String storeName;
	private Long departmentId;
	private String departmentName;

	public StoreInfo(Long storeId, String storeName, Long departmentId, String departmentName) {
		this.storeId = storeId;
		this.storeName = storeName;
		this.departmentId = departmentId;
		this.departmentName = departmentName;
	}
}
