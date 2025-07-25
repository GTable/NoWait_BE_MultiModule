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
public class StoreDetailReadResponse {
	private Long storeId;
	private Long waitingCount;
	private Boolean isWaiting;
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
	private Boolean isBookmarked;

	public static StoreDetailReadResponse fromEntity(Store store, List<StoreImageUploadResponse> allImages, String departmentName, Long waitingCount, Boolean isWaiting) {

		StoreImageUploadResponse profile = allImages.stream()
			.filter(image -> image.getImageType() == ImageType.PROFILE)
			.findFirst()
			.orElse(null);

		List<StoreImageUploadResponse> banners = allImages.stream()
			.filter(image -> image.getImageType() == ImageType.BANNER)
			.toList();

		return StoreDetailReadResponse.builder()
			.createdAt(store.getCreatedAt())
			.storeId(store.getStoreId())
			.waitingCount(waitingCount)
			.isWaiting(isWaiting)
			.departmentId(store.getDepartmentId())
			.departmentName(departmentName)
			.name(store.getName())
			.location(store.getLocation())
			.description(store.getDescription())
			.notice(store.getNotice())
			.openTime(store.getOpenTime())
			.isActive(store.getIsActive())
			.deleted(store.getDeleted())
			.profileImage(profile)
			.bannerImages(banners)
			.isBookmarked(false)
			.build();
	}

	public static StoreDetailReadResponse fromEntityWithBookmark(
		Store store, List<StoreImageUploadResponse> allImages, String departmentName, Long waitingCount, Boolean isBookmarked, Boolean isWaiting
	) {

		StoreImageUploadResponse profile = allImages.stream()
			.filter(image -> image.getImageType() == ImageType.PROFILE)
			.findFirst()
			.orElse(null);

		List<StoreImageUploadResponse> banners = allImages.stream()
			.filter(image -> image.getImageType() == ImageType.BANNER)
			.toList();

		return StoreDetailReadResponse.builder()
			.createdAt(store.getCreatedAt())
			.storeId(store.getStoreId())
			.waitingCount(waitingCount)
			.isWaiting(isWaiting)
			.departmentId(store.getDepartmentId())
			.departmentName(departmentName)
			.name(store.getName())
			.location(store.getLocation())
			.description(store.getDescription())
			.notice(store.getNotice())
			.openTime(store.getOpenTime())
			.isActive(store.getIsActive())
			.deleted(store.getDeleted())
			.profileImage(profile)
			.bannerImages(banners)
			.isBookmarked(isBookmarked)
			.build();
	}
}
