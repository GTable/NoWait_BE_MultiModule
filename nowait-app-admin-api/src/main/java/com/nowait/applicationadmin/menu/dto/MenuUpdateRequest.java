package com.nowait.applicationadmin.menu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MenuUpdateRequest {
	private String name;
	private String description;
	private Integer price;
}
