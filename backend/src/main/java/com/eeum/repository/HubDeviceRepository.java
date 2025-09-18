package com.eeum.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eeum.entity.HubDevice;

public interface HubDeviceRepository extends JpaRepository<HubDevice, String> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE eeum.hub_device
           SET user_home_id = :userHomeId
         WHERE hub_device_id = :hubDeviceId
           AND user_home_id IS NULL
        """, nativeQuery = true)
    int bindHubBySerial(@Param("hubDeviceId") String hubDeviceId,
                        @Param("userHomeId") Integer userHomeId);
    
    
    boolean existsByUserHomeId(Integer userHomeId);
    
    @Query("SELECT h.hubDeviceId FROM HubDevice h WHERE h.userHomeId = :userHomeId")
    Optional<String> findHubDeviceIdByUserHomeId(@Param("userHomeId") Integer userHomeId);
}
