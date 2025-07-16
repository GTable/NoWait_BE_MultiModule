package com.nowait.domaincorerdb.store.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nowait.domaincorerdb.store.entity.ImageType;
import com.nowait.domaincorerdb.store.entity.Store;
import com.nowait.domaincorerdb.store.entity.StoreImage;

@Repository
public interface StoreImageRepository extends JpaRepository<StoreImage, Long> {

	List<StoreImage> findByStore(Store store);

	Optional<StoreImage> findByStoreStoreIdAndImageType(Long storeId, ImageType imageType);
}
