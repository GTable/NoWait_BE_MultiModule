package com.nowait.domaincorerdb.department.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nowait.domaincorerdb.department.entity.Department;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
}
