package com.nowait.domainadminrdb.cancelOrder.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nowait.domainadminrdb.cancelOrder.entity.CancelOrder;

@Repository
public interface CancelOrderRepository extends JpaRepository<CancelOrder, Long> {
}
