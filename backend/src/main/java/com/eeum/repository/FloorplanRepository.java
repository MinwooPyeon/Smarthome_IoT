package com.eeum.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eeum.entity.Floorplan;

public interface FloorplanRepository extends JpaRepository<Floorplan, Integer> {
	
	// 주소로 평면도 조회
    @Query("""
            select f
            from Floorplan f
            join Home h on h.homeId = f.homeId
            join UserHome uh on uh.homeId = h.homeId
            where uh.userId = :userId
              and (:addressId is null or h.addressId = :addressId)
            order by f.createdAt desc
        """)
        List<Floorplan> findAllByUserIdAndAddressId(@Param("userId") Integer userId,
                                                    @Param("addressId") Integer addressId);
    
    
    // 특정 homeId의 평면도
    @Query("""
            select f
            from Floorplan f
            where f.homeId = :homeId
            order by f.createdAt desc
        """)
        List<Floorplan> findAllByHomeId(@Param("homeId") Integer homeId);
    
    
    // 사용자가 소속된 집들의 평면도 전체 조회
//    @Query("""
//            select f
//            from Floorplan f
//            join Home h on h.homeId = f.homeId
//            join UserHome uh on uh.homeId = h.homeId
//            where uh.userId = :userId
//            order by f.createdAt desc
//        """)
//        List<Floorplan> findAllByUserHomes(@Param("userId") Integer userId);
    
}