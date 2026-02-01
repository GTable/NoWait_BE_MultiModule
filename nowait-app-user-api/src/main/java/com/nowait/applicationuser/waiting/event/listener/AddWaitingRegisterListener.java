package com.nowait.applicationuser.waiting.event.listener;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.nowait.applicationuser.waiting.event.AddWaitingRegisterEvent;
import com.nowait.domaincoreredis.reservation.repository.WaitingRedisRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AddWaitingRegisterListener {

	private final WaitingRedisRepository waitingRedisRepository;

	@Async
	@TransactionalEventListener(
		classes = AddWaitingRegisterEvent.class,
		phase = TransactionPhase.AFTER_COMMIT
	)
	public void onAddWaitingRegister(AddWaitingRegisterEvent event) {
		waitingRedisRepository.addWaiting(event.getStoreId(), event.getUserId(), event.getTimestamp());
	}
}
