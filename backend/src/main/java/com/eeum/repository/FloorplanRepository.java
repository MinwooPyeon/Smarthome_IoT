package com.eeum.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eeum.entity.Floorplan;
import com.eeum.service.FloorplanService;

public interface FloorplanRepository extends JpaRepository<Floorplan, Integer> {
	
	// 주소로 평면도 목록 조회    
    @Query("""
    		  select
    		    f.floorplanId  as floorplanId,
    		    f.imageUrl     as imageUrl,
    		    f.createdAt    as createdAt,
    		    f.square       as square,
    		    f.floorplansX  as floorplansX,
    		    f.floorplansY  as floorplansY,
    		    h.homeId       as homeId,
    		    a.homeName     as homeName
    		  from Floorplan f
    		  join Home h on h.homeId = f.homeId
    		  join Address a on a.addressId = h.addressId
    		  where (:addressId is null or h.addressId = :addressId)
    		  order by f.createdAt desc
    		""")
    		List<FloorplanService.FloorplanRow> findAllForMapByAddressId(
    		  @Param("addressId") Integer addressId
    		);
    
    
    // 특정 homeId의 평면도
    @Query("""
            select f
            from Floorplan f
            where f.homeId = :homeId
            order by f.createdAt desc
        """)
        List<Floorplan> findAllByHomeId(@Param("homeId") Integer homeId);
    
    
    // homeId로 평면도 square 조회
    @Query(value = """
            SELECT f.square
              FROM eeum.floorplans f
             WHERE f.home_id = :homeId
             ORDER BY f.floorplan_id
             LIMIT 1
            """, nativeQuery = true)
    Optional<Double> findSquareByHomeId(@Param("homeId") Integer homeId);
}