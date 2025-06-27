package com.nowait.domainreservation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nowait.domainreservation.entity.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
	List<Reservation> findAllByStore_StoreIdOrderByRequestedAtAsc(Long storeId);
}
