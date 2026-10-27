package com.nowait.applicationuser.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "주점 대기 현황 정보 DTO")
public class StoreWaitingInfo {
	@Schema(description = "배너 첫번째 이미지", example = "https://cdn.gtable.com/profile/user1.jpg")
	private String bannerImageUrl;

	@Schema(description = "학과 이름", example = "경영학과")
	private String departmentName;

	@Schema(description = "주점 ID", example = "1")
	private String storeId;

	@Schema(description = "주점 publicCode", example = "ad2ndkald24d")
	private String publicCode;

	@Schema(description = "주점명", example = "노웨이팅 치킨")
	private String storeName;

	@Schema(description = "대기 인원 수", example = "12")
	private Long waitingCount;
}
