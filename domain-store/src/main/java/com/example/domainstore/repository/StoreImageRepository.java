package com.example.domainstore.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.domainstore.entity.Store;
import com.example.domainstore.entity.StoreImage;

@Repository
public interface StoreImageRepository extends JpaRepository<StoreImage, Long> {

	List<StoreImage> findByStore(Store store);
}
