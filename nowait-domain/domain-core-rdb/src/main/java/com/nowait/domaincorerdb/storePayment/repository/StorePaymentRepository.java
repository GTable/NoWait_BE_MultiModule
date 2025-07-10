package com.nowait.domaincorerdb.storePayment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nowait.domaincorerdb.storePayment.entity.StorePayment;

@Repository
public interface StorePaymentRepository extends JpaRepository<StorePayment, Long> {
	Optional<StorePayment> findByStoreId(Long storeId);
}
