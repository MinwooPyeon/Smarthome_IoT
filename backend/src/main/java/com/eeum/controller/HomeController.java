package com.eeum.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eeum.dto.response.AddressListResponse;
import com.eeum.service.HomeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "집 API", description = "지도마커용 집 API")
public class HomeController implements ControllerHelper {

    private final HomeService homeService;
    
    @Operation(
            summary = "주소로 집 정보 검색",
            description = "주소(detail) 키워드로 검색하여 addressId 기준으로 중복 제거한 결과를 반환합니다.(키워드 없으면 전체 조회)"
        )
        @GetMapping("/addresses/search")
        public ResponseEntity<?> search(
                @RequestParam(name = "keyword") String keyword
        ) {
            try {
                AddressListResponse body = homeService.listAddressMarkers(keyword);
                return handleSuccess(body, HttpStatus.OK);
            } catch (IllegalArgumentException e) {
                return handleFail(e, HttpStatus.BAD_REQUEST);
            } catch (Exception e) {
                return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }
