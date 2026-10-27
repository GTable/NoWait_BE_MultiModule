package com.nowait.applicationuser.store.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.nowait.domaincorerdb.store.entity.ImageType;
import com.nowait.domaincorerdb.store.entity.Store;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class StoreReadDto {
	private Long storeId;
	private Long departmentId;
	private String name;
	private String location;
	private String description;
	private StoreImageUploadResponse profileImage;
	private List<StoreImageUploadResponse> bannerImages;
	private Boolean isActive;
	private Boolean deleted;
	private LocalDateTime createdAt;

	public static StoreReadDto fromEntity(Store store, List<StoreImageUploadResponse> allImages) {

		StoreImageUploadResponse profile = allImages.stream()
			.filter(image -> image.getImageType() == ImageType.PROFILE)
			.findFirst()
			.orElse(null);

		List<StoreImageUploadResponse> banners = allImages.stream()
			.filter(image -> image.getImageType() == ImageType.BANNER)
			.toList();

		return StoreReadDto.builder()
			.createdAt(store.getCreatedAt())
			.storeId(store.getStoreId())
			.departmentId(store.getDepartmentId())
			.name(store.getName())
			.location(store.getLocation())
			.description(store.getDescription())
			.isActive(store.getIsActive())
			.deleted(store.getDeleted())
			.profileImage(profile)
			.bannerImages(banners)
			.build();
	}
}
