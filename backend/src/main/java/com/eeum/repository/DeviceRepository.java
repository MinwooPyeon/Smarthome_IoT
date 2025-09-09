package com.eeum.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eeum.entity.Device;

import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Integer> {

//	// roomName + deviceName로 deviceId 조회
//    @Query(value = """
//        select d.device_id
//        from device d
//        join room r on d.room_id = r.room_id
//        where d.user_id = :userId
//          and r.user_id = :userId
//          and lower(r.room_name) = lower(:roomName)
//          and lower(d.device_name) = lower(:deviceName)
//        limit 1
//        """, nativeQuery = true)
//    Optional<Integer> findDeviceId(@Param("userId") Integer userId, @Param("roomName") String roomName, @Param("deviceName") String deviceName);
//
//    
//    // 디바이스 전체/조건 목록 (active, type, roomName, deviceName 모두 AND로 필터)
//    @Query(value = """
//        select
//          d.device_id        as deviceId,
//          d.room_id          as roomId,
//          d.remote_id        as remoteId,
//          d.ir_device_id     as irDeviceId,
//          iri.brand          as brand,
//          iri.model          as model,
//          d.device_name      as deviceName,
//          iri.device_type    as type,
//          d.registered_at    as registeredAt,
//          d.device_detail::text as deviceDetail
//        from device d
//        left join ir_remoteir iri on d.remote_id = iri.remote_id
//        left join room r on d.room_id = r.room_id and r.user_id = :userId
//        where d.user_id = :userId
//          and ( :active is null or (
//                case lower(d.device_detail->>'onoff')
//                  when 'on' then true
//                  when 'off' then false
//                  else null
//                end
//              ) = :active )
//          and ( :type      is null or lower(iri.device_type) = lower(:type) )
//          and ( :roomName  is null or lower(r.room_name)     = lower(:roomName) )
//          and ( :deviceName is null or lower(d.device_name)  = lower(:deviceName) )
//        order by d.device_id
//        """, nativeQuery = true)
//    List<DeviceRow> findDeviceList(@Param("userId") Integer userId,
//                                       @Param("active") Boolean active,
//                                       @Param("type") String type,
//                                       @Param("roomName") String roomName,
//                                       @Param("deviceName") String deviceName);

	
	// 유저가 home에 소속인지 확인
    @Query(value = """
            SELECT EXISTS (
              SELECT 1
              FROM eeum.user_home uh
              WHERE uh.user_id = :userId
                AND uh.home_id = :homeId
            )
            """, nativeQuery = true)
        boolean existsUserHome(@Param("userId") Integer userId, @Param("homeId") Integer homeId);

    // room이 home에 속하는지 확인
    @Query(value = """
            SELECT EXISTS (
              SELECT 1
              FROM eeum.room r
              JOIN eeum.floorplans f ON f.floorplan_id = r.floorplan_id
              WHERE r.room_id = :roomId
                AND f.home_id = :homeId
            )
            """, nativeQuery = true)
        boolean existsRoomInHome(@Param("homeId") Integer homeId, @Param("roomId") Integer roomId);

    
    // roomId로 roomName 조회
    @Query(value = "SELECT r.room_name FROM eeum.room r WHERE r.room_id = :roomId LIMIT 1", nativeQuery = true)
    Optional<String> findRoomNameById(@Param("roomId") Integer roomId);
	
    // 평면도 좌표 입력
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
        INSERT INTO eeum.device_positions
          (x_coordinate, y_coordinate, device_id, room_id, home_id)
        VALUES (:x, :y, :deviceId, :roomId, :homeId)
        """, nativeQuery = true)
    void insertDevicePosition(@Param("x") Double x,
                              @Param("y") Double y,
                              @Param("deviceId") Integer deviceId,
                              @Param("roomId") Integer roomId,
                              @Param("homeId") Integer homeId);
    
    
    // 디바이스 단건 조회
    @Query(value = """
            SELECT
                d.device_id                    AS deviceId,
                dp.room_id                     AS roomId,
                d.remote_id                    AS remoteId,
                d.ir_device_id                 AS irDeviceId,
                irr.brand                      AS brand,
                irr.model                      AS model,
                irr.device_type                AS type,
                d.device_name                  AS deviceName,
                d.registered_at                AS registeredAt,
                CAST(d.device_detail AS TEXT)  AS deviceDetail
            FROM eeum.device d
            JOIN eeum.device_positions dp
              ON dp.device_id = d.device_id
            JOIN eeum.user_home uh
              ON uh.home_id = dp.home_id
            LEFT JOIN eeum.ir_remoteir irr
              ON irr.remote_id = d.remote_id
            WHERE uh.user_id = :userId
              AND d.device_id = :deviceId
            LIMIT 1
            """, nativeQuery = true)
        Optional<DeviceRow> findDevice(@Param("userId") Integer userId,
                                       @Param("deviceId") Integer deviceId);


    interface DeviceRow {
        Integer getDeviceId();
        Integer getRoomId();
        Integer getRemoteId();
        Integer getIrDeviceId();
        String  getBrand();
        String  getModel();
        String  getType();
        String  getDeviceName();
        Instant getRegisteredAt();
        String  getDeviceDetail(); 
    }
}
