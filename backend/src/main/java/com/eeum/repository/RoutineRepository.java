package com.eeum.repository;

import com.eeum.entity.Routine;

import io.lettuce.core.dynamic.annotation.Param;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

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
}
