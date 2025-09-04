package com.eeum.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eeum.dto.User;

public interface UserRepository extends JpaRepository<User, Integer> {

}
