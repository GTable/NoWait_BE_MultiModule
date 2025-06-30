package com.nowait.bookmark.repository;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nowait.store.entity.Store;
import com.nowait.bookmark.entity.Bookmark;
import com.nowait.user.entity.User;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark,Long> {
	boolean existsByUserAndStore(User user, Store store);

	Collection<Bookmark> findAllByUser(User user);
}
