package com.eeum.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eeum.dto.response.AddressItemResponse;
import com.eeum.dto.response.AddressListResponse;
import com.eeum.repository.HomeRepository;
import com.eeum.repository.HomeRepository.AddressProjection;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeService {
	
	private final HomeRepository homeRepository;
	
	// 지도 주소 마커 조회 (keyword가 있으면 해당 키워드로 검색)
    @Transactional(readOnly = true)
    public AddressListResponse listAddressMarkers(String keyword) {

        List<AddressProjection> rows =
                (keyword == null || keyword.isBlank())
                        ? homeRepository.findAllAddressDistinct()
                        : homeRepository.searchAddressMarkers(keyword.trim());

        List<AddressItemResponse> items = rows.stream()
                .map(p -> new AddressItemResponse(
                        p.getAddressId(),
                        p.getLongitude(),
                        p.getLatitude(),
                        p.getHomeName(),
                        p.getDetail()
                ))
                .collect(Collectors.toList());

        return new AddressListResponse(items);
    }
}