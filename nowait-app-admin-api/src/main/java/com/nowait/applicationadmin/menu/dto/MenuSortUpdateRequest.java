package com.nowait.applicationadmin.menu.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MenuSortUpdateRequest {
	@NotNull
	private Long menuId;
	@NotNull
	@PositiveOrZero
	private Long sortOrder;
}
