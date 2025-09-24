package com.eeum.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eeum.entity.Routine;

public interface RoutineRepository extends JpaRepository<Routine, Integer> {

   Optional<Routine> findByRoutineIdAndUserId(Integer routineId, Integer userId);

    // 단건 조회
   @Query("""
           select distinct r
           from Routine r
           left join fetch r.details d
           where r.routineId = :routineId and r.userId = :userId
           """)
    Optional<Routine> findWithDetailsByRoutineIdAndUserId(@Param("routineId") Integer routineId,
                                                          @Param("userId") Integer userId);

    // 전체 조회
   @Query("""
           select distinct r
           from Routine r
           left join fetch r.details d
           where r.userId = :userId
           """)
    List<Routine> findAllWithDetailsByUserId(@Param("userId") Integer userId);
   
   
   @Query(value = """
		    WITH k AS (
		      SELECT (now() AT TIME ZONE 'Asia/Seoul')::time(0) AS kst_time
		    )
		    SELECT r.routine_id AS routineId,
		           r.user_id    AS userId
		      FROM eeum.routine r, k
		     WHERE COALESCE(r.trigger_type, FALSE) = TRUE
		       AND ((r.routine_weekday & :weekdayMask) <> 0)
		       AND r.act_time = k.kst_time
		    """, nativeQuery = true)
		List<DueRoutineRow> findDueRoutinesDailyKst(@Param("weekdayMask") int weekdayMask);

    	
    	interface DueRoutineRow {
    	    Integer getRoutineId();
    	    Integer getUserId();
    	}
}
