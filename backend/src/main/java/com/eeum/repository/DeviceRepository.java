package com.eeum.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eeum.entity.Device;

public interface DeviceRepository extends JpaRepository<Device, Integer> {
	
    // 디바이스 전체/조건 목록 (power, type, roomName, deviceName 모두 AND로 필터)
	@Query(value = """
		    SELECT DISTINCT
		        d.device_id                   AS deviceId,
		        dp.room_id                    AS roomId,
		        d.ir_device_id                AS irDeviceId,
		        irr.brand                     AS brand,
		        d.model                       AS model,
		        irr.device_type               AS deviceType,
		        d.device_name                 AS deviceName,
		        d.registered_at               AS registeredAt,
		        CAST(d.device_detail AS TEXT) AS deviceDetail,
		        dp.x_coordinate               AS x,
		        dp.y_coordinate               AS y
		    FROM eeum.device d
		    JOIN eeum.device_positions dp
		      ON dp.device_id = d.device_id
		    LEFT JOIN eeum.ir_remoteir irr
		      ON irr.model = d.model
		    WHERE dp.home_id = :homeId
		      AND EXISTS (
		            SELECT 1
		              FROM eeum.user_home uh
		             WHERE uh.home_id = :homeId
		               AND uh.user_id = :userId
		          )
		      AND (:type IS NULL OR irr.device_type ILIKE '%' || :type || '%')
		      AND (:roomName IS NULL OR EXISTS (
		            SELECT 1
		              FROM eeum.room rm
		             WHERE rm.room_id = dp.room_id
		               AND rm.room_name ILIKE '%' || :roomName || '%'
		          ))
		      AND (:deviceName IS NULL OR d.device_name ILIKE '%' || :deviceName || '%')
		      AND (
		          :power IS NULL
		          OR CAST(COALESCE(d.device_detail->>'power', 'false') AS boolean) = :power
		      )
		    ORDER BY d.device_id DESC
		    """, nativeQuery = true)
		List<DeviceRow> findDeviceList(
		        @Param("userId") Integer userId,
		        @Param("homeId") Integer homeId,
		        @Param("power") Boolean power,
		        @Param("type") String type,
		        @Param("roomName") String roomName,
		        @Param("deviceName") String deviceName
		);



    // user_home + 같은 방(room) + 같은 기기 존재 여부
    @Query(value = """
            SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END
              FROM device d
              JOIN device_positions dp ON dp.device_id = d.device_id
              JOIN ir_remoteir irm      ON irm.model = d.model
             WHERE d.user_home_id = :userHomeId
               AND dp.room_id     = :roomId
               AND LOWER(irm.device_type) = LOWER(:deviceType)
            """, nativeQuery = true)
        boolean existsDeviceInRoomByDeviceType(@Param("userHomeId") Integer userHomeId,
                                               @Param("roomId") Integer roomId,
                                               @Param("deviceType") String deviceType);
	
	// 유저가 home에 소속인지 확인
    @Query(value = """
            SELECT uh.user_home_id
              FROM eeum.user_home uh
             WHERE uh.user_id = :userId
               AND uh.home_id = :homeId
             LIMIT 1
            """, nativeQuery = true)
        Optional<Integer> findUserHomeId(@Param("userId") Integer userId,
                                         @Param("homeId") Integer homeId);
    
    
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
    @Modifying
    @Query(value = """
    INSERT INTO eeum.device_positions
        (x_coordinate, y_coordinate, device_id, room_id, home_id, model)
    VALUES (:x, :y, :deviceId, :roomId, :homeId, :model)
    """, nativeQuery = true)
	int insertDevicePosition(@Param("x") Double x,
	                         @Param("y") Double y,
	                         @Param("deviceId") Integer deviceId,
	                         @Param("roomId") Integer roomId,
	                         @Param("homeId") Integer homeId,
	                         @Param("model") String model);
    
    
    // 디바이스 단건 조회
    @Query(value = """
    	    SELECT
    	        d.device_id                  AS deviceId,
    	        dp.room_id                   AS roomId,
    	        d.ir_device_id               AS irDeviceId,
    	        irr.brand                    AS brand,
    	        d.model                      AS model,
    	        irr.device_type              AS type,
    	        d.device_name                AS deviceName,
    	        d.registered_at              AS registeredAt,
    	        CAST(d.device_detail AS TEXT) AS deviceDetail,
    		   	dp.x_coordinate               AS x, 
			  	dp.y_coordinate               AS y 
    	    FROM eeum.device d
    	    JOIN eeum.device_positions dp
    	      ON dp.device_id = d.device_id
    	    JOIN eeum.user_home uh
    	      ON uh.home_id = dp.home_id
    	    LEFT JOIN eeum.ir_remoteir irr
    	      ON irr.model = d.model
    	    WHERE uh.user_id = :userId
    	      AND d.device_id = :deviceId
    	    LIMIT 1
    	    """, nativeQuery = true)
    	Optional<DeviceRow> findDevice(
    	        @Param("userId") Integer userId,
    	        @Param("deviceId") Integer deviceId
    	);
    
    // 디바이스가 사용자의 소유인지 확인
    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                  FROM eeum.device d
                  JOIN eeum.user_home uh ON uh.user_home_id = d.user_home_id
                 WHERE d.device_id = :deviceId
                   AND uh.user_id   = :userId
            )
            """, nativeQuery = true)
        boolean userOwnsDevice(@Param("userId") Integer userId,
                               @Param("deviceId") Integer deviceId);
    
    // 디바이스 삭제
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM eeum.routine_detail WHERE device_id = :deviceId", nativeQuery = true)
    int deleteRoutineDetails(@Param("deviceId") Integer deviceId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM eeum.command WHERE device_id = :deviceId", nativeQuery = true)
    int deleteCommands(@Param("deviceId") Integer deviceId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM eeum.device_positions WHERE device_id = :deviceId", nativeQuery = true)
    int deletePositions(@Param("deviceId") Integer deviceId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM eeum.device WHERE device_id = :deviceId", nativeQuery = true)
    int deleteDeviceHard(@Param("deviceId") Integer deviceId);
    

    // 디바이스 평면도 위치 수정
    @Query(value = "SELECT position_id FROM eeum.device_positions WHERE device_id = :deviceId", nativeQuery = true)
    Optional<Integer> findPositionId(@Param("deviceId") Integer deviceId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE eeum.device_positions
           SET x_coordinate = :x,
               y_coordinate = :y,
               room_id      = :roomId,
               home_id      = :homeId
         WHERE device_id    = :deviceId
        """, nativeQuery = true)
    int updateDevicePosition(@Param("deviceId") Integer deviceId,
                             @Param("homeId") Integer homeId,
                             @Param("roomId") Integer roomId,
                             @Param("x") Double x,
                             @Param("y") Double y);
    
    
    // 유저 집의 디바이스 위치 목록 조회
    @Query(value = """
            SELECT dp.position_id   AS positionId,
                   dp.device_id     AS deviceId,
                   uh.home_id       AS homeId,
                   dp.room_id       AS roomId,
                   dp.x_coordinate  AS x,
                   dp.y_coordinate  AS y
              FROM eeum.device_positions dp
              JOIN eeum.device d
                ON d.device_id = dp.device_id
              JOIN eeum.user_home uh
                ON uh.user_home_id = d.user_home_id
             WHERE uh.user_id = :userId
               AND uh.home_id = :homeId
             ORDER BY dp.position_id
            """, nativeQuery = true)
        List<DeviceLocationRow> findDeviceLocationsInHome(@Param("userId") Integer userId,
                                                          @Param("homeId") Integer homeId);

    
    interface DeviceRow {
        Integer getDeviceId();
        Integer getRoomId();
        String getIrDeviceId();
        String  getBrand();
        String  getModel();
        String  getDeviceType();
        String  getDeviceName();
        Instant getRegisteredAt();
        String  getDeviceDetail(); 
        Double  getX();
        Double  getY();
    }
    
    
    interface DeviceLocationRow {
        Integer getPositionId();
        Integer getDeviceId();
        Integer getHomeId();
        Integer getRoomId();
        Double  getX();
        Double  getY();
    }
    
    
    @Query(value = """
            SELECT uh.home_id
              FROM eeum.device d
              JOIN eeum.user_home uh
                ON uh.user_home_id = d.user_home_id
             WHERE d.device_id = :deviceId
             LIMIT 1
            """, nativeQuery = true)
    Optional<Integer> findHomeIdByDeviceIdViaUserHome(@Param("deviceId") Integer deviceId);
    
    @Query(value = """
            SELECT dp.home_id
              FROM eeum.device_positions dp
             WHERE dp.device_id = :deviceId
             LIMIT 1
            """, nativeQuery = true)
    Optional<Integer> findHomeIdByDeviceId(@Param("deviceId") Integer deviceId);
    
}
