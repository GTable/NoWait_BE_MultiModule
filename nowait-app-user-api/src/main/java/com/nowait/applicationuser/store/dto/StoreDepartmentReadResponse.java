package com.nowait.applicationuser.store.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class StoreDepartmentReadResponse {

	private List<StorePageReadResponse> storePageReadDtos;
	private boolean hasNext;

	public static StoreDepartmentReadResponse of(List<StorePageReadResponse> storePageReadResponses, boolean hasNext) {
		return StoreDepartmentReadResponse.builder()
			.storePageReadDtos(storePageReadResponses)
			.hasNext(hasNext)
			.build();
	}
}
