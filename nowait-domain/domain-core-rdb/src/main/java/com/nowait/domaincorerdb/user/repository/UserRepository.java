package com.nowait.domaincorerdb.user.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nowait.domaincorerdb.user.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByNickname(String nickName);
    List<User> findByIdIn(Collection<Long> ids);
    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);
}
