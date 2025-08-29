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
public class StoreSearchResponse {
	private Long storeId;
	private String publicCode;
	private Long waitingCount;
	private Long departmentId;
	private String departmentName;
	private String name;
	private String openTime;
	private StoreImageUploadResponse profileImage;
	private Boolean isActive;
	private Boolean deleted;
	private LocalDateTime createdAt;

	public static StoreSearchResponse fromEntity(Store store, List<StoreImageUploadResponse> profileImage, String departmentName, Long waitingCount) {

		StoreImageUploadResponse profile = profileImage.stream()
			.filter(image -> image.getImageType() == ImageType.PROFILE)
			.findFirst()
			.orElse(null);

		return StoreSearchResponse.builder()
			.storeId(store.getStoreId())
			.publicCode(store.getPublicCode())
			.waitingCount(waitingCount)
			.departmentId(store.getDepartmentId())
			.departmentName(departmentName)
			.name(store.getName())
			.openTime(store.getOpenTime())
			.profileImage(profile)
			.isActive(store.getIsActive())
			.deleted(store.getDeleted())
			.createdAt(store.getCreatedAt())
			.build();
	}
}
