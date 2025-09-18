package com.eeum.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eeum.entity.IrEventLog;

public interface IrEventLogRepository extends JpaRepository<IrEventLog, Long> {
	
}