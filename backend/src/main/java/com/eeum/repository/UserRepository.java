package com.eeum.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eeum.entity.User;

public interface UserRepository extends JpaRepository<User, Integer> {
    boolean existsByLoginId(String loginId);
    boolean existsByEmail(String email);
}