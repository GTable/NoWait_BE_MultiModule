package com.nowait.applicationadmin.menu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MenuSortUpdateRequest {
	private Long menuId;
	private Long sortOrder;
}
