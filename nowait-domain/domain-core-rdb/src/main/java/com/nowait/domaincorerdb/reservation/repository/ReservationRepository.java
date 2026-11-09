package com.nowait.domaincorerdb.reservation.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nowait.common.enums.ReservationStatus;
import com.nowait.domaincorerdb.reservation.entity.Reservation;
import com.nowait.domaincorerdb.store.entity.Store;
import com.nowait.domaincorerdb.user.entity.User;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, ReservationCustomRepository {
	List<Reservation> findAllByStore_StoreIdOrderByRequestedAtAsc(Long storeId);

	boolean existsByUserAndStoreAndStatusIn(User user, Store store, List<ReservationStatus> statuses);

	boolean existsReservationByReservationNumber(String reservationNumber);

	Optional<Reservation> findFirstByStore_StoreIdAndUserIdAndStatusInAndRequestedAtBetweenOrderByRequestedAtDesc(
		Long storeId, Long userId, List<ReservationStatus> statuses, LocalDateTime start, LocalDateTime end);

	List<Reservation> findAllByStore_StoreIdAndStatusInAndRequestedAtBetween(
		Long storeId, List<ReservationStatus> statuses, LocalDateTime start, LocalDateTime end);

	@Query("select r from Reservation r join fetch r.store join fetch r.user "
		+ "where r.status in :statuses and r.requestedAt >= :from")
	List<Reservation> findActiveSince(@Param("statuses") List<ReservationStatus> statuses,
		@Param("from") LocalDateTime from);

	// 웨이팅 취소 처리
	Optional<Reservation> findReservationByReservationNumber(String reservationNumber);
}
