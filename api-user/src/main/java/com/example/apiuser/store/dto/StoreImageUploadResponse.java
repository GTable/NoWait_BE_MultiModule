package com.example.apiuser.store.dto;

import com.example.domainstore.entity.StoreImage;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoreImageUploadResponse {
	private final Long id;
	private final String imageUrl;

	public static StoreImageUploadResponse fromEntity(StoreImage storeImage) {
		return StoreImageUploadResponse.builder()
			.id(storeImage.getId())
			.imageUrl(storeImage.getImageUrl())
			.build();
	}
}
