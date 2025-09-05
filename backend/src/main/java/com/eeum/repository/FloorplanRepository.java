package com.eeum.repository;

import com.eeum.entity.Floorplans;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FloorplanRepository extends JpaRepository<Floorplans, Integer> {
	
}