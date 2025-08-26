package com.nowait.domaincorerdb.store.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nowait.domaincorerdb.store.entity.Store;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long>, StoreCustomRepository {

	Optional<Store> findByStoreIdAndDeletedFalse(Long storeId);

	Optional<Store> findByPublicCodeAndDeletedFalse(String publicCode);

	Slice<Store> findAllByDeletedFalseOrderByStoreIdAsc(Pageable pageable);

	@Query(value = """
        SELECT DISTINCT s.*
          FROM stores s
          LEFT JOIN departments d ON s.department_id = d.id
         WHERE s.deleted = false
           AND (
             MATCH(s.name) AGAINST(:kw)
          OR MATCH(d.name) AGAINST(:kw)
           )
        """,
		nativeQuery = true)
	List<Store> searchByKeywordNative(@Param("kw") String booleanKeyword);

	List<Store> findAllByStoreIdInOrderByStoreIdAsc(List<Long> storeIds);

	@Query(value = """
		SELECT s
		  FROM Store s
		  LEFT JOIN Department d ON s.departmentId = d.id
		 WHERE s.deleted = false
		   AND s.storeId IN :ids
		""")
	List<Store> findAllWithDepartmentByStoreIdIn(@Param("ids") List<Long> ids);
}
