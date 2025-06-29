package com.example.apiadmin.store.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class StoreReadResponse {

	private List<StoreReadDto> storeReadDto;
	private boolean hasNext;

	public static StoreReadResponse of(List<StoreReadDto> storeReadDto, boolean hasNext) {
		return StoreReadResponse.builder()
			.storeReadDto(storeReadDto)
			.hasNext(hasNext)
			.build();
	}
}
