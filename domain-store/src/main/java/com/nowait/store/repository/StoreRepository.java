package com.nowait.store.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nowait.store.entity.Store;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

	List<Store> findAllByDeletedFalse();

	Optional<Store> findByStoreIdAndDeletedFalse(Long storeId);

	List<Store> findByNameContainingIgnoreCaseAndDeletedFalse(String name);

	Slice<Store> findAllByDeletedFalseOrderByStoreIdAsc(Pageable pageable);
}
