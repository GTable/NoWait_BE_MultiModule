package com.nowait.applicationadmin.menu.dto;


import com.nowait.domaincorerdb.menu.entity.Menu;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MenuCreateRequest {

	@NotNull
	private Long storeId;
	private String adminDisplayName;
	@NotNull
	private String name;
	@NotNull
	private String description;
	@NotNull
	private Integer price;

	public Menu toEntity() {
		return Menu.builder()
			.storeId(storeId)
			.adminDisplayName(adminDisplayName != null ? adminDisplayName : name)
			.name(name)
			.description(description)
			.price(price)
			.isSoldOut(false)
			.sortOrder(0L)
			.deleted(false)
			.build();
	}
}

