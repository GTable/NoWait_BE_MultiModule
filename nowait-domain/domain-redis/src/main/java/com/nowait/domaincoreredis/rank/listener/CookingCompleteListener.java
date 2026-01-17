package com.nowait.domaincoreredis.rank.listener;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.nowait.domaincoreredis.rank.service.MenuCounterService;
import com.nowait.nowaitevent.order.event.CookingCompleteEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CookingCompleteListener {
	private final MenuCounterService menuCounterService;

	@Async
	@TransactionalEventListener(
		classes = CookingCompleteEvent.class,
		phase = TransactionPhase.AFTER_COMMIT
	)
	public void onCookingComplete(CookingCompleteEvent event) {
		Long storeId = event.getStoreId();
		for (CookingCompleteEvent.Item item : event.getItems()) {
			menuCounterService.incrementMenuCounter(
				item.getMenuId(),
				storeId,
				item.getQuantity()
			);
		}
	}
}
