package com.nowait.applicationadmin.menu.dto;

import java.time.LocalDateTime;

import com.nowait.domaincorerdb.menu.entity.Menu;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class MenuCreateResponse {
	private Long menuId;
	private Long storeId;
	private String adminDisplayName;
	private String name;
	private String description;
	private Integer price;
	private Boolean isSoldOut;
	private Long sortOrder;
	private Boolean deleted;
	private LocalDateTime createdAt;

	public static MenuCreateResponse fromEntity(Menu menu) {
		return MenuCreateResponse.builder()
			.createdAt(menu.getCreatedAt())
			.adminDisplayName(menu.getAdminDisplayName())
			.menuId(menu.getId())
			.storeId(menu.getStoreId())
			.adminDisplayName(menu.getAdminDisplayName())
			.name(menu.getName())
			.description(menu.getDescription())
			.price(menu.getPrice())
			.isSoldOut(menu.getIsSoldOut())
			.sortOrder(menu.getSortOrder())
			.deleted(menu.getDeleted())
			.build();
	}
}
