package com.nowait.domaincorerdb.department.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<com.nowait.domaincorerdb.department.entity.Department, Long> {
}
