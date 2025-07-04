package com.nowait.domainuserrdb.bookmark.repository;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nowait.domaincorerdb.store.entity.Store;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domainuserrdb.bookmark.entity.Bookmark;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark,Long> {
	boolean existsByUserAndStore(User user, Store store);

	Collection<Bookmark> findAllByUser(User user);
}
