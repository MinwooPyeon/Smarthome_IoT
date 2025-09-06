package com.eeum.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eeum.entity.Floorplan;

public interface FloorplanRepository extends JpaRepository<Floorplan, Integer> {
	
	// homeId 기준으로 평면도 목록 조회
	List<Floorplan> findByHomeId(Integer homeId);
	
    @Query(value = """
        select
          fp.floorplan_id as floorplanId,
          fp.image_url    as imageUrl
        from floorplans fp
        join user_home uh
          on uh.floorplan_id = fp.floorplan_id
        where uh.user_id = :userId
          and uh.floorplan_id is not null
        order by fp.floorplan_id
        """, nativeQuery = true)
    List<FloorplanSummary> findUserSelectedFloorplans(@Param("userId") Integer userId);

    interface FloorplanSummary {
        Integer getFloorplanId();
        String  getImageUrl();
    }
}