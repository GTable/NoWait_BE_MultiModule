package com.nowait.domaincorerdb.reservation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nowait.domaincorerdb.reservation.entity.Reservation;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
	List<Reservation> findAllByStore_StoreIdOrderByRequestedAtAsc(Long storeId);
}
