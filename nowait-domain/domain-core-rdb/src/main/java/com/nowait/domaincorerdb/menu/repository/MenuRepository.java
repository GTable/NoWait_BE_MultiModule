package com.nowait.domaincorerdb.menu.repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nowait.domaincorerdb.menu.entity.Menu;
import com.nowait.domaincorerdb.menu.entity.MenuImage;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {
	List<Menu> findAllByStoreIdAndDeletedFalse(Long storeId);
	List<Menu> findAllByStoreIdAndDeletedFalseOrderBySortOrder(Long storeId);
	Optional<Menu> findByStoreIdAndIdAndDeletedFalse(Long storeId, Long menuId);
	Optional<Menu> findByIdAndDeletedFalse(Long menuId);

	// 이미지가 아직 없는 메뉴도 보여야 하므로 LEFT JOIN (행: [Menu, MenuImage 또는 null])
	@Query(value = """
		SELECT m, i
		  FROM Menu m
		  LEFT JOIN MenuImage i ON i.menu = m
		 WHERE m.storeId = :storeId
		   AND m.deleted = false
		 ORDER BY m.sortOrder, i.id
		""")
	List<Object[]> findAllWithImageRowsByStoreId(@Param("storeId") Long storeId);

	/**
	 * 주점의 메뉴와 이미지를 쿼리 한 번으로 가져온다 (메뉴마다 이미지를 따로 조회하던 N+1 대체)
	 * sortOrder 순서를 유지하고, 이미지가 없는 메뉴는 빈 목록이다
	 */
	default Map<Menu, List<MenuImage>> findAllWithImagesByStoreId(Long storeId) {
		return findAllWithImageRowsByStoreId(storeId).stream()
			.collect(Collectors.groupingBy(row -> (Menu)row[0], LinkedHashMap::new,
				Collectors.flatMapping(row -> Stream.ofNullable((MenuImage)row[1]), Collectors.toList())));
	}
}
