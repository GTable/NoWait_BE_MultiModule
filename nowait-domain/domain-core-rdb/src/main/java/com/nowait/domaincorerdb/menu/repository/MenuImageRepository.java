package com.nowait.domaincorerdb.menu.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nowait.domaincorerdb.menu.entity.Menu;
import com.nowait.domaincorerdb.menu.entity.MenuImage;

@Repository
public interface MenuImageRepository extends JpaRepository<MenuImage, Long> {
	List<MenuImage> findByMenu(Menu menu);
}
