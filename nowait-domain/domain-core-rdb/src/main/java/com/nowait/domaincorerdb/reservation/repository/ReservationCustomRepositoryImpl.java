package com.nowait.domaincorerdb.reservation.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.nowait.common.enums.ReservationStatus;
import com.nowait.domaincorerdb.department.entity.QDepartment;
import com.nowait.domaincorerdb.reservation.dto.GetMyWaitingBaseDto;
import com.nowait.domaincorerdb.reservation.dto.QGetMyWaitingBaseDto;
import com.nowait.domaincorerdb.reservation.entity.QReservation;
import com.nowait.domaincorerdb.store.entity.ImageType;
import com.nowait.domaincorerdb.store.entity.QStore;
import com.nowait.domaincorerdb.store.entity.QStoreImage;
import com.nowait.domaincorerdb.user.entity.QUser;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ReservationCustomRepositoryImpl implements ReservationCustomRepository {

	private final JPAQueryFactory queryFactory;

	private final QReservation reservation = QReservation.reservation;
	private final QStore store = QStore.store;
	private final QDepartment department = QDepartment.department;
	private final QUser user = QUser.user;
	private final QStoreImage storeImage = QStoreImage.storeImage;


	@Override
	public List<GetMyWaitingBaseDto> findMyWaitingInfo(Long userId) {

		LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
		LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

		QStoreImage subStoreImage = new QStoreImage("subStoreImage");

		return queryFactory
			.select(new QGetMyWaitingBaseDto(
				reservation.reservationNumber,
				store.storeId,
				store.name,
				department.name,
				reservation.partySize,
				reservation.status,
				reservation.requestedAt,
				store.location,
				storeImage.imageUrl,

				JPAExpressions
					.select(subStoreImage.imageUrl)
					.from(subStoreImage)
					.where(
						subStoreImage.store.storeId.eq(store.storeId),
						subStoreImage.imageType.eq(ImageType.BANNER)
					)
					.limit(1)
			))
			.from(reservation)
			.join(reservation.store, store)
			.join(reservation.user, user)
			.leftJoin(department)
			.on(store.departmentId.eq(department.id))
			.leftJoin(storeImage)
			.on(
				storeImage.store.storeId.eq(store.storeId),
				storeImage.imageType.eq(ImageType.PROFILE)
			)
			.where(
				reservation.user.id.eq(userId),
				reservation.status.in(
					ReservationStatus.WAITING,
					ReservationStatus.CALLING
				),
				reservation.requestedAt.between(startOfDay, endOfDay),
				store.deleted.isFalse()
			)
			.fetch();
	}
}
