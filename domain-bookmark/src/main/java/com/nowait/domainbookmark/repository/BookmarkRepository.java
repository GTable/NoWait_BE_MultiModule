package com.nowait.domainbookmark.repository;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.domainstore.entity.Store;
import com.nowait.domainbookmark.entity.Bookmark;
import com.nowaiting.user.entity.User;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark,Long> {
	boolean existsByUserAndStore(User user, Store store);

	Collection<Bookmark> findAllByUser(User user);
}
