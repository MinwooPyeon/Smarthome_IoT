package com.eeum.repository;

import com.eeum.entity.Home;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HomeRepository extends JpaRepository<Home, Integer> {
	
	//모든 집을 address_id 기준으로 집계하여 목록 반환
    @Query(value = """
            SELECT
                h.address_id      AS addressId,
                AVG(h.longitude)  AS longitude,
                AVG(h.latitude)   AS latitude
            FROM eeum.home h
            GROUP BY h.address_id
            ORDER BY h.address_id
            """, nativeQuery = true)
        List<AddressProjection> findAllAddressDistinct();

    
    @Query(value = """
            SELECT
                a.address_id         AS addressId,
                AVG(h.longitude)     AS longitude,
                AVG(h.latitude)      AS latitude
            FROM eeum.addresses a
            JOIN eeum.home h ON h.address_id = a.address_id
            WHERE a.detail ILIKE CONCAT('%', :q, '%')
            GROUP BY a.address_id
            ORDER BY a.address_id
            """, nativeQuery = true)
        List<AddressProjection> searchAddressMarkers(
                @Param("q") String keyword
        );
    
    
    interface AddressProjection {
        Integer getAddressId();
        Double getLongitude();
        Double getLatitude();
    }
}



