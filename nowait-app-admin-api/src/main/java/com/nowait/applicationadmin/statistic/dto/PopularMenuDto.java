package com.nowait.applicationadmin.statistic.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PopularMenuDto {
	private Long menuId;
	private String menuName;
	private Long soldCount;
	private String imageUrl;
}
