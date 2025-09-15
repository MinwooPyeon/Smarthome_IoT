package com.eeum.controller;

import com.eeum.dto.response.FloorplanListResponse;
import com.eeum.service.FloorplanService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "평면도 API", description = "평면도 API")
public class FloorplanController implements ControllerHelper {

    private final FloorplanService floorplanService;

    // 평면도 등록
    @Operation(summary = "평면도 등록", description = "사용자가 평면도를 선택하여 집과 평면도를 등록합니다.")
    @PostMapping("/homes/{homeId}/floorplans")
    public ResponseEntity<?> create(
    		@PathVariable(name = "homeId") Integer homeId
    ) {
    	try {
	    	Integer userId = 1;
	        Integer id = floorplanService.create(userId, homeId);
	        return handleSuccess(Map.of("homeId", id));
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (SecurityException e) {
            return handleFail(e, HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // 특정 집의 평면도 목록
    @Operation(summary = "주소별 평면도 목록 조회", description = "집의 평면도 목록을 조회합니다.")
    @GetMapping("/addresses/{addressId}/floorplans")
    public  ResponseEntity<?> listByHome(
            @PathVariable(name = "addressId") Integer addressId
    ) {
        try {
            Integer userId = 1;
            FloorplanListResponse body = floorplanService.listByAddressId(userId, addressId);
            return handleSuccess(body);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (SecurityException e) {
            return handleFail(e, HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Operation(summary = "등록한 평면도 삭제", description = "평면도를 삭제합니다.(집 계약관계도 삭제)")
    @DeleteMapping("/homes/{homeId}/floorplan")
    public  ResponseEntity<?> deleteMyMembershipByHome(
            @PathVariable(name = "homeId") Integer homeId
    ) {
        try {
            Integer userId = 1;
            floorplanService.deleteUserHomeByHome(userId, homeId);
            return handleSuccess(Map.of("deleted", true));
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (SecurityException e) {
            return handleFail(e, HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Operation(summary = "특정 집 평면도", description = "사용자가 해당 집에 설정한 평면도 리스트를 조회합니다.")
    @GetMapping("/users/homes/{homeId}/floorplans")
    public ResponseEntity<?> listMyFloorplansByHome(@PathVariable(name = "homeId") Integer homeId) {
        try {
            Integer userId = 1;
            FloorplanListResponse body = floorplanService.listByUserAndHome(userId, homeId);
            return handleSuccess(body, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (SecurityException e) {
            return handleFail(e, HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
//    @Operation(
//            summary = "내 평면도 목록",
//            description = "사용자가 등록한 집들의 평면도를 반환합니다."
//        )
//        @GetMapping("/users/floorplans")
//        public ResponseEntity<?> listMyFloorplansForCurrentUser() {
//            try {
//                Integer userId = 1;
//                FloorplanListResponse body = floorplanService.listByUserHomes(userId);
//                return handleSuccess(body, HttpStatus.OK);
//            } catch (IllegalArgumentException e) {
//                return handleFail(e, HttpStatus.BAD_REQUEST);
//            } catch (Exception e) {
//                return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
//            }
//        }
    
}
