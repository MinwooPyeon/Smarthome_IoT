package com.eeum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.eeum.entity.RoutineIcon;


@Repository
public interface IconRepository extends JpaRepository<RoutineIcon, Integer> {

}

