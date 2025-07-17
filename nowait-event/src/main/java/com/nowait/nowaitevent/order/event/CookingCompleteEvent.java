package com.nowait.nowaitevent.order.event;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CookingCompleteEvent {
	private final Long storeId;
	private final List<Item> items;

	@Getter
	@RequiredArgsConstructor
	public static class Item {
		private final Long menuId;
		private final int quantity;
	}
}
