package com.nowait.applicationadmin.store.dto;

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
			.departmentId(departmentId)
			.name(name)
			.location(location)
			.description(description)
			.isActive(false)
			.deleted(false)
			.build();
	}
}
