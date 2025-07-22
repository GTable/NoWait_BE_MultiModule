package com.nowait.domaincorerdb.reservation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nowait.common.enums.ReservationStatus;
import com.nowait.domaincorerdb.reservation.entity.Reservation;
import com.nowait.domaincorerdb.store.entity.Store;
import com.nowait.domaincorerdb.user.entity.User;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
	List<Reservation> findAllByStore_StoreIdOrderByRequestedAtAsc(Long storeId);
	boolean existsByUserAndStoreAndStatusIn(User user, Store store, List<ReservationStatus> statuses);

	Optional<Reservation> findByStore_StoreIdAndUserId(Long storeId, Long userId);

	List<Reservation> findAllByStore_StoreId(Long storeId);

}
