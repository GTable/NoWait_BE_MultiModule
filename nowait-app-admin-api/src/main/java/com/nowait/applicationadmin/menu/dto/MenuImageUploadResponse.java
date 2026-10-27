package com.nowait.applicationadmin.menu.dto;

import com.nowait.domaincorerdb.menu.entity.MenuImage;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MenuImageUploadResponse {
	private final Long id;
	private final String imageUrl;

	public static MenuImageUploadResponse fromEntity(MenuImage menuImage) {
		return MenuImageUploadResponse.builder()
			.id(menuImage.getId())
			.imageUrl(menuImage.getImageUrl())
			.build();
	}
}
