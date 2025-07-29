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
public class StorePageReadResponse {
	private Long storeId;
	private Long waitingCount;
	private Long departmentId;
	private String departmentName;
	private String name;
	private String location;
	private String description;
	private String notice;
	private String openTime;
	private StoreImageUploadResponse profileImage;
	private List<StoreImageUploadResponse> bannerImages;
	private Boolean isActive;
	private Boolean deleted;
	private LocalDateTime createdAt;

	public static StorePageReadResponse fromEntity(Store store, List<StoreImageUploadResponse> allImages, String departmentName, Long waitingCount) {

		StoreImageUploadResponse profile = allImages.stream()
			.filter(image -> image.getImageType() == ImageType.PROFILE)
			.findFirst()
			.orElse(null);

		List<StoreImageUploadResponse> banners = allImages.stream()
			.filter(image -> image.getImageType() == ImageType.BANNER)
			.toList();

		return StorePageReadResponse.builder()
			.storeId(store.getStoreId())
			.waitingCount(waitingCount)
			.departmentId(store.getDepartmentId())
			.departmentName(departmentName)
			.name(store.getName())
			.location(store.getLocation())
			.description(store.getDescription())
			.notice(store.getNotice())
			.openTime(store.getOpenTime())
			.profileImage(profile)
			.bannerImages(banners)
			.isActive(store.getIsActive())
			.deleted(store.getDeleted())
			.createdAt(store.getCreatedAt())
			.build();
	}
}
