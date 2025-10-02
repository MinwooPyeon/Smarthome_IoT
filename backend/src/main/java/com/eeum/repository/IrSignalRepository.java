package com.eeum.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.eeum.entity.IrSignal;

@Repository
public interface IrSignalRepository extends JpaRepository<IrSignal, Integer> {
    
	// 자동 쿼리 생성
	Optional<IrSignal> findByModelAndName(String model, String name);
    
    @Query("""
            SELECT s.signalId
            FROM IrSignal s
            WHERE s.model = :model
              AND s.buttonId = :buttonId
            """)
        Optional<Integer> findSignalIdByModelAndButtonId(
            @Param("model") String model,
            @Param("buttonId") Integer buttonId
        );
}