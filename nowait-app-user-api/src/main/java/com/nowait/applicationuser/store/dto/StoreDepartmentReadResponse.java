package com.nowait.applicationuser.store.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class StoreDepartmentReadResponse {

	private List<StorePageReadDto> storePageReadDtos;
	private boolean hasNext;

	public static StoreDepartmentReadResponse of(List<StorePageReadDto> storePageReadDtos, boolean hasNext) {
		return StoreDepartmentReadResponse.builder()
			.storePageReadDtos(storePageReadDtos)
			.hasNext(hasNext)
			.build();
	}
}
