package com.eeum.repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eeum.entity.IrEventLog;

public interface IrEventLogRepository extends JpaRepository<IrEventLog, Long> {
	
    interface LogRow {
        String getDeviceName();
        Instant getEventTime();
        String getKind();
        Integer getRoomId();
        String getRoomName();
    }

    @Query(value = """
            SELECT 
                d.device_name  AS deviceName,
                el.event_time  AS eventTime,
                el.kind        AS kind,
                r.room_id      AS roomId,
                r.room_name    AS roomName
            FROM eeum.ir_event_log el
            JOIN eeum.device d
              ON d.ir_device_id = el.ir_device_id
             AND d.model        = el.model
            JOIN eeum.user_home uh
              ON uh.user_home_id = d.user_home_id
            /* 디바이스 최신 위치(방) 1건만 조인 */
            LEFT JOIN LATERAL (
                SELECT dp2.room_id
                  FROM eeum.device_positions dp2
                 WHERE dp2.device_id = d.device_id
                 ORDER BY dp2.position_id DESC
                 LIMIT 1
            ) dp ON true
            LEFT JOIN eeum.room r
                   ON r.room_id = dp.room_id
            WHERE uh.user_id = :userId
              AND uh.home_id = :homeId
            ORDER BY el.event_time DESC
            LIMIT :limit
            """, nativeQuery = true)
        List<LogRow> findRecentLogsByUserAndHome(
                @Param("userId") Integer userId,
                @Param("homeId") Integer homeId,
                @Param("limit") int limit
        );
}