package com.nowait.menu.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nowait.menu.entity.Menu;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {
	List<Menu> findAllByStoreIdAndDeletedFalse(Long storeId);
	Optional<Menu> findByStoreIdAndIdAndDeletedFalse(Long storeId, Long menuId);
	Optional<Menu> findByIdAndDeletedFalse(Long menuId);
}
