package com.eeum.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;  

import java.util.List;

import com.eeum.entity.UserHome;

public interface UserHomeRepository extends JpaRepository<UserHome, Integer> {

	// userId가 해당 addressId의 집에 접근 권한이 있는지 확인
    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                  FROM eeum.user_home uh
                  JOIN eeum.home h ON h.home_id = uh.home_id
                 WHERE uh.user_id = :userId
                   AND h.address_id = :addressId
            )
            """, nativeQuery = true)
        boolean existsByUserIdAndAddressId(@Param("userId") Integer userId,
                                           @Param("addressId") Integer addressId);

    
    boolean existsByUserIdAndHomeId(Integer userId, Integer homeId);

    
    // userhome 삭제
    void deleteByUserIdAndHomeId(Integer userId, Integer homeId);
    
    
    // 유저의 집 목록 조회
    @Query(value = """
            SELECT h.home_id   AS homeId,
                   a.home_name AS homeName
              FROM eeum.user_home uh
              JOIN eeum.home       h ON uh.home_id   = h.home_id
              JOIN eeum.addresses  a ON h.address_id = a.address_id
             WHERE uh.user_id = :userId
             ORDER BY h.home_id
            """, nativeQuery = true)
        List<HomeIdNameProjection> findHomeIdAndNameByUserId(@Param("userId") Integer userId);


        interface HomeIdNameProjection {
            Integer getHomeId();
            String getHomeName();
        }
    }