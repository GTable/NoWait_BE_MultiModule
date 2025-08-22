package com.nowait.applicationuser.menu.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class MenuReadResponse {

	private String storeName;
	private List<MenuReadDto> menuReadDto;

	public static MenuReadResponse of(String storeName, List<MenuReadDto> menuReadDto) {
		return MenuReadResponse.builder()
			.storeName(storeName)
			.menuReadDto(menuReadDto)
			.build();
	}
}
