package com.nowait.domaincorerdb.order.repository;

import static com.nowait.domaincorerdb.order.entity.OrderStatus.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.nowait.domaincorerdb.order.dto.OrderSalesSumResponse;
import com.nowait.domaincorerdb.order.entity.QUserOrder;
import com.querydsl.jpa.impl.JPAQueryFactory;

public class OrderCustomRepositoryImpl implements OrderCustomRepository {

	private final JPAQueryFactory queryFactory;

	public OrderCustomRepositoryImpl(JPAQueryFactory queryFactory) {
		this.queryFactory = queryFactory;
	}

	QUserOrder u = QUserOrder.userOrder;

	@Override
	public OrderSalesSumResponse findSalesSumByStoreId(Long storeId) {
		// 1. 날짜 기준 설정 (시작은 자정, 끝은 다음 날 자정)
		LocalDate today = LocalDate.now();
		LocalDate yesterday = today.minusDays(1);

		LocalDateTime todayStart = today.atStartOfDay();
		LocalDateTime todayEnd = today.plusDays(1).atStartOfDay(); // 내일 00:00:00

		LocalDateTime yesterdayStart = yesterday.atStartOfDay();
		LocalDateTime yesterdayEnd = today.atStartOfDay();

		// 2. 오늘 매출 합산
		Integer todaySum = queryFactory
			.select(u.totalPrice.sum())
			.from(u)
			.where(
				u.store.storeId.eq(storeId),
				u.createdAt.goe(todayStart),
				u.createdAt.lt(todayEnd),
				u.status.eq(COOKED)
			)
			.fetchOne();

		// 3. 어제 매출 합산
		Integer yesterdaySum = queryFactory
			.select(u.totalPrice.sum())
			.from(u)
			.where(
				u.store.storeId.eq(storeId),
				u.createdAt.goe(yesterdayStart),
				u.createdAt.lt(yesterdayEnd),
				u.status.eq(COOKED)
			)
			.fetchOne();

		Integer previousDaySales = queryFactory
			.select(u.totalPrice.sum())
			.from(u)
			.where(
				u.store.storeId.eq(storeId),
				u.createdAt.lt(yesterdayEnd),
				u.status.eq(COOKED)
			)
			.fetchOne();

		// null 방어 처리
		if (todaySum == null) todaySum = 0;
		if (yesterdaySum == null) yesterdaySum = 0;
		if (previousDaySales == null) previousDaySales = 0;

		// 4. 응답 객체 생성
		return new OrderSalesSumResponse(storeId, todaySum, yesterdaySum, previousDaySales);
	}
}
