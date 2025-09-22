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
//    @Query("""
//            select distinct r
//            from Routine r
//            left join fetch r.details d
//            where r.userId = :userId
//            """)
//     List<Routine> findAllWithDetailsByUserId(@Param("userId") Integer userId);
//    
//    @Query(value = """
//    	    SELECT r.routine_id AS routineId,
//    	           r.user_id    AS userId
//    	      FROM eeum.routine r
//    	     WHERE COALESCE(r.trigger_type, FALSE) = TRUE           -- 알람형만
//    	       AND r.act_time IS NOT NULL
//    	       AND ((r.routine_weekday & :weekdayMask) <> 0)        -- 오늘 요일 비트와 교집합
//    	       AND EXTRACT(HOUR   FROM (r.act_time AT TIME ZONE 'Asia/Seoul')) = :kstHour
//    	       AND EXTRACT(MINUTE FROM (r.act_time AT TIME ZONE 'Asia/Seoul')) = :kstMinute
//    	    """, nativeQuery = true)
//    	List<DueRoutineRow> findDueRoutinesKst(@Param("kstHour") int kstHour,
//    	                                       @Param("kstMinute") int kstMinute,
//    	                                       @Param("weekdayMask") int weekdayMask);
   @Query(value = """
		   WITH k AS (
		     SELECT date_trunc('minute', now() AT TIME ZONE 'Asia/Seoul') AS kst_min
		   )
		   SELECT r.routine_id AS routineId,
		          r.user_id    AS userId
		   FROM eeum.routine r, k
		   WHERE COALESCE(r.trigger_type, FALSE) = TRUE
		     AND r.act_time IS NOT NULL
		     AND ((r.routine_weekday & :weekdayMask) <> 0)
		     AND r.act_time >= (k.kst_min AT TIME ZONE 'Asia/Seoul')
		     AND r.act_time <  ((k.kst_min + interval '1 minute') AT TIME ZONE 'Asia/Seoul')
		   """, nativeQuery = true)
		   List<DueRoutineRow> findDueRoutinesKst(@Param("weekdayMask") int weekdayMask);

    	
    	interface DueRoutineRow {
    	    Integer getRoutineId();
    	    Integer getUserId();
    	}
}
