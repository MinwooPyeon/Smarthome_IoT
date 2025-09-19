package com.eeum.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eeum.entity.IrEventLog;

public interface IrEventLogRepository extends JpaRepository<IrEventLog, Long> {
	
    // 디바이스 로그 목록 조회
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
    
    
    // 에너지 사용량 조회
    interface EnergyRow {
        String  getIrDeviceId();
        String  getModel();
        Instant getEventTime();
        String  getKind(); // power_on|power_off
        String  getDeviceType();
        Double  getPowerConsumption();
    }

    // power_on / power_off / on / off 만 가져와서 상태전이 계산에 사용
    @Query(value = """
            SELECT
                l.ir_device_id      AS irDeviceId,
                l.model             AS model,
                l.event_time        AS eventTime,
                l.kind              AS kind,
                r.device_type       AS deviceType,
                r.power_consumption AS powerConsumption
            FROM eeum.ir_event_log l
            JOIN eeum.device d
              ON d.ir_device_id = l.ir_device_id
             AND d.model        = l.model
            JOIN eeum.user_home uh
              ON uh.user_home_id = d.user_home_id
            LEFT JOIN eeum.ir_remoteir r
              ON r.model = l.model
            WHERE uh.user_id = :userId
              AND uh.home_id = :homeId
              AND l.event_time >= :from
              AND l.event_time <  :to
              AND (
                    lower(l.kind) LIKE 'power_%%'
                 OR lower(l.kind) IN ('on','off')
              )
            ORDER BY l.event_time ASC
            """, nativeQuery = true)
        List<EnergyRow> findPowerEventsInRange(
                @Param("userId") Integer userId,
                @Param("homeId") Integer homeId,
                @Param("from")   Instant from,
                @Param("to")     Instant to
        );
    

    // 집계 시작 시점 직전의 마지막 전원 이벤트를 기기별 1개씩 조회해 초기 on/off 상태를 결정하는 데 사용
    @Query(value = """
            SELECT DISTINCT ON (l.ir_device_id, l.model)
                l.ir_device_id      AS irDeviceId,
                l.model             AS model,
                l.event_time        AS eventTime,
                l.kind              AS kind,
                r.device_type       AS deviceType,
                r.power_consumption AS powerConsumption
            FROM eeum.ir_event_log l
            JOIN eeum.device d
              ON d.ir_device_id = l.ir_device_id
             AND d.model        = l.model
            JOIN eeum.user_home uh
              ON uh.user_home_id = d.user_home_id
            LEFT JOIN eeum.ir_remoteir r
              ON r.model = l.model
            WHERE uh.user_id   = :userId
              AND uh.home_id   = :homeId
              AND l.event_time < :from
              AND (
                    lower(l.kind) LIKE 'power_%%'
                 OR lower(l.kind) IN ('on','off')
              )
            ORDER BY l.ir_device_id, l.model, l.event_time DESC
            """, nativeQuery = true)
        List<EnergyRow> findLastPowerEventBefore(
                @Param("userId") Integer userId,
                @Param("homeId") Integer homeId,
                @Param("from")   Instant from
        );
    }