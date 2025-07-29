package com.nowait.applicationadmin.store.dto;

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
	private Long departmentId;
	private String departmentName;
	private String name;
	private String location;
	private String description;
	private String noticeTitle;
	private String noticeContent;
	private String openTime;
	private StoreImageUploadResponse profileImage;
	private List<StoreImageUploadResponse> bannerImages;
	private Boolean isActive;
	private Boolean deleted;
	private LocalDateTime createdAt;

	public static StoreDetailReadResponse fromEntity(Store store, List<StoreImageUploadResponse> allImages, String departmentName) {

		StoreImageUploadResponse profile = allImages.stream()
			.filter(image -> image.getImageType() == ImageType.PROFILE)
			.findFirst()
			.orElse(null);

		List<StoreImageUploadResponse> banners = allImages.stream()
			.filter(image -> image.getImageType() == ImageType.BANNER)
			.toList();

		return StoreDetailReadResponse.builder()
			.storeId(store.getStoreId())
			.departmentId(store.getDepartmentId())
			.departmentName(departmentName)
			.name(store.getName())
			.location(store.getLocation())
			.description(store.getDescription())
			.noticeTitle(store.getNoticeTitle())
			.noticeContent(store.getNoticeContent())
			.openTime(store.getOpenTime())
			.profileImage(profile)
			.bannerImages(banners)
			.isActive(store.getIsActive())
			.deleted(store.getDeleted())
			.createdAt(store.getCreatedAt())
			.build();
	}
}
