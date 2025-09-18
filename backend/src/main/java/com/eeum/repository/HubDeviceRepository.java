package com.eeum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eeum.entity.HubDevice;

public interface HubDeviceRepository extends JpaRepository<HubDevice, Integer> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE eeum.hub_device
           SET user_home_id = :userHomeId
         WHERE device_addr = CAST(:deviceAddr AS inet)
           AND user_home_id IS NULL
        """, nativeQuery = true)
    int bindHubByAddr(@Param("deviceAddr") String deviceAddr,
                      @Param("userHomeId") Integer userHomeId);
    
    
    boolean existsByUserHomeId(Integer userHomeId);
}
