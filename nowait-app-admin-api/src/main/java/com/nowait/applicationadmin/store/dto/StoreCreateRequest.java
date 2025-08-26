package com.nowait.applicationadmin.store.dto;

import com.nowait.common.token.TokenGenerator;
import com.nowait.domaincorerdb.store.entity.Store;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StoreCreateRequest {

	@NotNull
	private Long departmentId;

	@NotBlank
	private String name;

	private String location;

	private String description;

	public Store toEntity() {
		return Store.builder()
			.publicCode(TokenGenerator.base62(12))
			.departmentId(departmentId)
			.name(name)
			.location(location)
			.description(description)
			.noticeTitle("제목을 입력해주세요.")
			.noticeContent("내용을 입력해주세요.")
			.openTime("00002359")
			.isActive(false)
			.deleted(false)
			.build();
	}
}
