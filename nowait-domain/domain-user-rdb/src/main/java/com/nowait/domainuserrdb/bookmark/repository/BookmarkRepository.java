package com.nowait.domainuserrdb.bookmark.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nowait.domaincorerdb.store.entity.Store;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domainuserrdb.bookmark.entity.Bookmark;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark,Long> {
	boolean existsByUserAndStoreAndDeletedFalse(User user, Store store);

	Optional<Bookmark> findByUserAndStoreAndDeletedFalse(User user, Store store);

	Optional<Bookmark> findByUserAndStore(User user, Store store);

	Collection<Bookmark> findAllByUserAndDeletedFalse(User user);

	List<Bookmark> findStoreIdByUserAndDeletedFalse(User user);

	@Query("""
      select b
        from Bookmark b
       where b.user.id = :userId
         and b.store.storeId = :storeId
         and b.deleted = false
    """)
	Optional<Bookmark> findActiveByUserIdAndStoreId(
		@Param("storeId") Long storeId,
		@Param("userId")  Long userId
	);
}
