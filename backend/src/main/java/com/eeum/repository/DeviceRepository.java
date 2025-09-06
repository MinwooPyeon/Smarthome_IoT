//package com.eeum.repository;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//
//import com.eeum.entity.Device;
//
//import java.time.Instant;
//import java.util.List;
//import java.util.Optional;
//
//public interface DeviceRepository extends JpaRepository<Device, Integer> {
//
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
//
//    // 디바이스 단건 조회
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
//          and d.device_id = :deviceId
//        limit 1
//        """, nativeQuery = true)
//    Optional<DeviceRow> findDevice(@Param("userId") Integer userId,
//                                       @Param("deviceId") Integer deviceId);
//
// 
//    
//    interface DeviceRow {
//        Integer getDeviceId();
//        Integer getRoomId();
//        Integer getRemoteId();
//        Integer getIrDeviceId();
//        String  getBrand();
//        String  getModel();
//        String  getDeviceName();
//        String  getType();
//        Instant getRegisteredAt();
//        String  getDeviceDetail();
//    }
//}
