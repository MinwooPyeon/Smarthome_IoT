package com.eeum.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eeum.entity.UserHome;

public interface UserHomeRepository extends JpaRepository<UserHome, Integer> {

	UserHome findByUserIdAndHomeId(Integer userId, Integer homeId);

}
