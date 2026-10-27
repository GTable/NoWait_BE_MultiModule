package com.nowait.domaincorerdb.store.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "이미지 타입 Enum")
public enum ImageType {
	@Schema(description = "주점 프로필 사진")
	PROFILE("프로필 사진"),

	@Schema(description = "주점 배너 사진")
	BANNER("배너 사진"),

	@Schema(description = "주점 배너 사진")
	NONE("이미지 없음");

	private final String description;
}
