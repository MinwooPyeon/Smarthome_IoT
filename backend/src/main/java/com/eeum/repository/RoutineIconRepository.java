package com.eeum.repository;

import com.eeum.entity.RoutineIcon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoutineIconRepository extends JpaRepository<RoutineIcon, Integer> {
    boolean existsById(Integer iconId);
}
