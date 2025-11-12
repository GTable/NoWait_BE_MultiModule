package com.nowait.domaincorerdb.order.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nowait.domaincorerdb.order.entity.UserOrder;

@Repository
public interface OrderRepository extends JpaRepository<UserOrder, Long> {
	boolean existsBySignatureAndCreatedAtAfter(String signature, LocalDateTime createdAt);

	@EntityGraph(attributePaths = {"store", "orderItems", "orderItems.menu"})
	List<UserOrder> findByStore_PublicCodeAndTableIdAndSessionId(String publicCode, Long tableId, String sessionId);

	@EntityGraph(attributePaths = {"orderItems", "orderItems.menu"})
	List<UserOrder> findAllByStore_StoreIdAndCreatedAtBetween(Long storeId, LocalDateTime startDateTime, LocalDateTime endDateTime);
}
