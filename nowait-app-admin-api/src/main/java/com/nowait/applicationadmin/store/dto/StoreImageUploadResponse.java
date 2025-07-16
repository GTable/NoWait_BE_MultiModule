package com.nowait.applicationadmin.store.dto;

import com.nowait.domaincorerdb.store.entity.ImageType;
import com.nowait.domaincorerdb.store.entity.StoreImage;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoreImageUploadResponse {
	private final Long id;
	private final String imageUrl;
	private final ImageType imageType;

	public static StoreImageUploadResponse fromEntity(StoreImage storeImage) {
		return StoreImageUploadResponse.builder()
			.id(storeImage.getId())
			.imageUrl(storeImage.getImageUrl())
			.imageType(storeImage.getImageType())
			.build();
	}
}
