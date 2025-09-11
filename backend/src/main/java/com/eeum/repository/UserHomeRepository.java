package com.eeum.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import com.eeum.entity.UserHome;

import org.springframework.data.repository.query.Param;  
import jakarta.transaction.Transactional;

public interface UserHomeRepository extends JpaRepository<UserHome, Integer> {

	// userId가 해당 addressId의 집에 접근 권한이 있는지
	@Query(value = """
		    select exists(
		      select 1
		      from eeum.user_home uh
		      join eeum.home h on h.home_id = uh.home_id
		      where uh.user_id = :userId and h.address_id = :addressId
		    )
		""", nativeQuery = true)
		boolean existsByUserIdAndAddressId(@Param("userId") Integer userId,
		                                   @Param("addressId") Integer addressId);
	
        boolean existsByUserIdAndHomeId(Integer userId, Integer homeId);
        
        // userhome 삭제
        @Transactional
        void deleteByUserIdAndHomeId(Integer userId, Integer homeId);
    }