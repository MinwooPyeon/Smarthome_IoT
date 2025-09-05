package com.eeum.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eeum.entity.Floorplan;

public interface FloorplanRepository extends JpaRepository<Floorplan, Integer> {
	
	// homeId 기준으로 평면도 목록 조회
	List<Floorplan> findByHomeId(Integer homeId);
}