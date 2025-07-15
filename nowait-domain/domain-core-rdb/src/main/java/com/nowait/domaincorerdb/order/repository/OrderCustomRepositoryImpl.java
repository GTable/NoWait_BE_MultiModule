package com.nowait.domaincorerdb.order.repository;

import static com.nowait.domaincorerdb.order.entity.OrderStatus.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.nowait.domaincorerdb.department.entity.QDepartment;
import com.nowait.domaincorerdb.order.dto.OrderSalesSumDetail;
import com.nowait.domaincorerdb.order.dto.TopSalesStoresDetail;
import com.nowait.domaincorerdb.order.entity.QUserOrder;
import com.nowait.domaincorerdb.store.entity.QStore;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;

public class OrderCustomRepositoryImpl implements OrderCustomRepository {

	private final JPAQueryFactory queryFactory;

	public OrderCustomRepositoryImpl(JPAQueryFactory queryFactory) {
		this.queryFactory = queryFactory;
	}

	private static final QUserOrder u = QUserOrder.userOrder;
	private static final QStore s = QStore.store;
	private static final QDepartment d = QDepartment.department;

	@Override
	public OrderSalesSumDetail findSalesSumByStoreId(Long storeId) {
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
		if (todaySum == null)
			todaySum = 0;
		if (yesterdaySum == null)
			yesterdaySum = 0;
		if (previousDaySales == null)
			previousDaySales = 0;

		// 4. 응답 객체 생성
		return new OrderSalesSumDetail(storeId, todaySum, yesterdaySum, previousDaySales);
	}

	@Override
	public List<TopSalesStoresDetail> getTop4PlusMine(Long userStoreId) {
		LocalDate today = LocalDate.now();
		LocalDateTime todayStart = today.atStartOfDay();
		LocalDateTime todayEnd = today.plusDays(1).atStartOfDay(); // 내일 00:00:00

		// 1. 전체 주점 및 순위 맵
		List<Tuple> allStores = getAllStores(todayStart, todayEnd);
		Map<Long, Integer> rankMap = getRankMap(allStores);

		// 2. 내 주점의 순위가 5위 이하인지 확인
		Integer userRank = rankMap.get(userStoreId);

		// 3. 상위 5개 주점 조회 (내 주점 포함 여부는 나중에 확인)
		List<TopSalesStoresDetail> result = getTopNStores(allStores, rankMap, 5);
		Set<Long> already = result.stream()
			.map(TopSalesStoresDetail::getStoreId)
			.collect(Collectors.toSet());

		// 4. 내 주점 추가 (내 주점이 포함되지 않으면 추가)
		TopSalesStoresDetail myStore = getMyStoreDetail(allStores, rankMap, userStoreId);
		if (myStore == null)
			myStore = getZeroSalesStore(userStoreId);

		if (myStore != null && !already.contains(userStoreId)) {
			// 내 주점이 5위 이하로 밀린 경우
			if (userRank > 5) {
				result = result.stream()
					.sorted(Comparator.comparing(TopSalesStoresDetail::getStoreRank,
						Comparator.nullsLast(Comparator.naturalOrder())))
					.limit(4)
					.collect(Collectors.toList());
				// 5위 자리에 내 주점 추가하고 나머지 4개만 보여줌
				result.add(myStore);
			}
		}

		// 5. 결과가 6개가 되지 않도록 자르고 5개로 제한
		result = result.stream()
			.sorted(Comparator.comparing(TopSalesStoresDetail::getStoreRank,
				Comparator.nullsLast(Comparator.naturalOrder())))
			.collect(Collectors.toList());

		return result;
	}

	private List<Tuple> getAllStores(LocalDateTime todayStart, LocalDateTime todayEnd) {
		return queryFactory
			.select(u.store.storeId, u.store.name, u.store.departmentId, u.totalPrice.sum())
			.from(u)
			.where(
				u.createdAt.goe(todayStart),
				u.createdAt.lt(todayEnd),
				u.status.eq(COOKED)
			)
			.groupBy(u.store.storeId, u.store.name, u.store.departmentId)
			.orderBy(u.totalPrice.sum().desc())
			.fetch();
	}

	private Map<Long, Integer> getRankMap(List<Tuple> allStores) {
		Map<Long, Integer> rankMap = new HashMap<>();
		int rank = 1;
		for (Tuple t : allStores) {
			rankMap.put(t.get(u.store.storeId), rank++);
		}
		return rankMap;
	}

	private List<TopSalesStoresDetail> getTopNStores(List<Tuple> allStores, Map<Long, Integer> rankMap, int n) {
		// 1. departmentId 목록을 추출
		Set<Long> departmentIds = allStores.stream()
			.map(t -> t.get(u.store.departmentId))
			.collect(Collectors.toSet());

		// 2. departmentId에 대한 학과 이름을 한번에 가져오기
		Map<Long, String> departmentNameMap = getDepartmentNames(departmentIds);

		List<TopSalesStoresDetail> list = new ArrayList<>();
		for (int i = 0; i < Math.min(n, allStores.size()); i++) {
			Tuple t = allStores.get(i);
			Long storeId = t.get(u.store.storeId);
			String storeName = t.get(u.store.name);
			Long departmentId = t.get(u.store.departmentId);

			// 학과 이름 가져오기
			String departmentName = departmentNameMap.getOrDefault(departmentId, "Unknown");

			list.add(new TopSalesStoresDetail(
				storeId,
				storeName,
				t.get(u.store.departmentId),
				departmentName,
				t.get(u.totalPrice.sum()),
				rankMap.get(storeId).longValue()
			));
		}
		return list;
	}

	private TopSalesStoresDetail getMyStoreDetail(List<Tuple> allStores, Map<Long, Integer> rankMap, Long userStoreId) {
		Tuple myStore = allStores.stream()
			.filter(t -> t.get(u.store.storeId).equals(userStoreId))
			.findFirst()
			.orElse(null);

		Long departmentId = myStore.get(u.store.departmentId);

		// departmentId에 대한 학과 이름을 한번에 가져오기
		Map<Long, String> departmentNameMap = getDepartmentNames(Set.of(departmentId));
		String departmentName = departmentNameMap.getOrDefault(departmentId, "Unknown");

		if (myStore == null)
			return null;
		Long storeId = myStore.get(u.store.storeId);
		return new TopSalesStoresDetail(
			storeId,
			myStore.get(u.store.name),
			myStore.get(u.store.departmentId),
			departmentName,
			myStore.get(u.totalPrice.sum()),
			rankMap.get(storeId).longValue()
		);
	}

	private TopSalesStoresDetail getZeroSalesStore(Long userStoreId) {
		Tuple zeroTuple = queryFactory
			.select(s.storeId, s.name, s.departmentId)
			.from(s)
			.where(s.storeId.eq(userStoreId))
			.fetchOne();

		if (zeroTuple == null)
			return null;

		Long departmentId = zeroTuple.get(u.store.departmentId);
		// departmentId에 대한 학과 이름을 한번에 가져오기
		Map<Long, String> departmentNameMap = getDepartmentNames(Set.of(departmentId));
		String departmentName = departmentNameMap.getOrDefault(departmentId, "Unknown");

		return new TopSalesStoresDetail(
			zeroTuple.get(s.storeId),
			zeroTuple.get(s.name),
			zeroTuple.get(s.departmentId),
			departmentName,
			0,
			null
		);
	}

	// 한 번에 학과 이름을 가져오는 메서드
	private Map<Long, String> getDepartmentNames(Set<Long> departmentIds) {
		List<Tuple> departmentTuples = queryFactory
			.select(d.Id, d.name)
			.from(d)
			.where(d.Id.in(departmentIds))
			.fetch();

		Map<Long, String> departmentNameMap = new HashMap<>();
		for (Tuple t : departmentTuples) {
			departmentNameMap.put(t.get(d.Id), t.get(d.name));
		}
		return departmentNameMap;
	}
}
