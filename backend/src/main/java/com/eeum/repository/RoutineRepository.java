package com.eeum.repository;

import com.eeum.entity.Routine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoutineRepository extends JpaRepository<Routine, Integer> {

    List<Routine> findAllByUserIdOrderByRoutineIdAsc(Integer userId);

    Optional<Routine> findByRoutineIdAndUserId(Integer routineId, Integer userId);

    boolean existsByRoutineIdAndUserId(Integer routineId, Integer userId);
}
