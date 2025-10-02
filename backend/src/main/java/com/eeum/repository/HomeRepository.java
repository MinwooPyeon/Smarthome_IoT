package com.eeum.repository;

import com.eeum.entity.Home;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HomeRepository extends JpaRepository<Home, Integer> {
	
	//모든 집을 address_id 기준으로 집계하여 목록 반환
    @Query(value = """
            select distinct a.address_id as addressId,
                            h.longitude  as longitude,
                            h.latitude   as latitude,
                            a.home_name  as homeName,
                            a.detail     as detail
            from eeum.home h
            join eeum.addresses a on a.address_id = h.address_id
            """, nativeQuery = true)
        List<AddressProjection> findAllAddressDistinct();

    
    // 키워드 검색 (detail 또는 home_name)
    @Query(value = """
            select a.address_id as addressId,
                   h.longitude  as longitude,
                   h.latitude   as latitude,
                   a.home_name  as homeName,
                   a.detail     as detail
            from eeum.home h
            join eeum.addresses a on a.address_id = h.address_id
            where (:keyword is null or
                  lower(a.detail) like lower(concat('%', :keyword, '%')) or
                  lower(a.home_name) like lower(concat('%', :keyword, '%')))
            """, nativeQuery = true)
    List<AddressProjection> searchAddressMarkers(@Param("keyword") String keyword);

    
    interface AddressProjection {
        Integer getAddressId();
        Double getLongitude();
        Double getLatitude();
        String getHomeName();
        String getDetail();
    }
}



