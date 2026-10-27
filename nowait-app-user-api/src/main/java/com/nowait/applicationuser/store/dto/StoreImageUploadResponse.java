package com.nowait.applicationuser.store.dto;

import com.nowait.domaincorerdb.store.entity.ImageType;
import com.nowait.domaincorerdb.store.entity.StoreImage;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoreImageUploadResponse {
	private final Long id;
	private final Long storeId;
	private final String imageUrl;
	private final ImageType imageType;

	public static StoreImageUploadResponse fromEntity(StoreImage storeImage) {
		return StoreImageUploadResponse.builder()
			.id(storeImage.getId())
			.storeId(storeImage.getStore().getStoreId())
			.imageUrl(storeImage.getImageUrl())
			.imageType(storeImage.getImageType())
			.build();
	}
}
