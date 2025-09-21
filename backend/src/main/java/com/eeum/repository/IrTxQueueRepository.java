package com.eeum.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eeum.entity.IrTxQueue;

public interface IrTxQueueRepository extends JpaRepository<IrTxQueue, UUID> {
	@Modifying
	@Query("""
	    UPDATE IrTxQueue t
	    SET t.status = :status,
	        t.lastError = :lastError
	    WHERE t.txId = :txId
	""")
	void updateStatusAndErrorByTxId(@Param("txId") int txId,
	                                @Param("status") String status,
	                                @Param("lastError") String lastError);
}