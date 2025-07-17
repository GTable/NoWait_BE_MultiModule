package com.nowait.nowaitevent.order.event;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CookingCompleteEvent {

	@NotNull
	private final Long storeId;

	@NotNull
	private final List<Item> items;

	@Getter
	@RequiredArgsConstructor
	public static class Item {

		@NotNull
		private final Long menuId;

		@NotNull
		private final int quantity;
	}
}
