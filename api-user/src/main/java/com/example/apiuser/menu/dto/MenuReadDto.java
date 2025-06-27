package com.example.apiuser.menu.dto;

import java.util.List;

import com.example.menu.entity.Menu;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class MenuReadDto {
	private Long menuId;
	private Long storeId;
	private String name;
	private String description;
	private Integer price;
	private List<MenuImageUploadResponse> images;

	public static MenuReadDto fromEntity(Menu menu, List<MenuImageUploadResponse> images) {
		return MenuReadDto.builder()
			.menuId(menu.getId())
			.storeId(menu.getStoreId())
			.name(menu.getName())
			.description(menu.getDescription())
			.price(menu.getPrice())
			.images(images)
			.build();
	}
}
