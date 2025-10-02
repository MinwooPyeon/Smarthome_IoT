package com.eeum.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eeum.dto.response.RoomItemResponse;
import com.eeum.dto.response.UserHomeItemResponse;
import com.eeum.dto.response.UserHomeListResponse;
import com.eeum.service.UserHomeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "User-Home API", description = "사용자의 집 API")
@RestController
@RequestMapping("/api/user-home")
@RequiredArgsConstructor
public class UserHomeController implements ControllerHelper {
	
    private final UserHomeService userHomeService;
	
    // 유저 집 목록 조회
    @Operation(summary = "유저 집 목록 조회", description = "유저가 가진 집의 목록을 보여줍니다.")
    @GetMapping("/address")
    public ResponseEntity<?> listMyHomes() {
        try {
            Integer userId = 1;
            List<UserHomeItemResponse> homes = userHomeService.listUserHomes(userId);
            return handleSuccess(new UserHomeListResponse(homes), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    
    // 유저 대표 집 주소 변경
    @Operation(summary = "대표 집 주소 변경", description = "표시할 대표 집 주소를 변경합니다.")
    @PutMapping("/{homeId}/primary")
    public ResponseEntity<?> changePrimaryHome(@PathVariable(name = "homeId") Integer homeId) {
        try {
            Integer userId = 1;
            Integer res = userHomeService.changePrimaryHome(userId, homeId);
            return handleSuccess(Map.of("homeId", res, "massage", "대표 집이 변경되었습니다."), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Operation(summary = "홈의 방 목록", description = "homeId에 해당하는 방 목록을 반환합니다.")
    @GetMapping("/{homeId}/rooms")
    public ResponseEntity<?> listRoomsByHomeId(@PathVariable(name = "homeId") Integer homeId) {
        Integer userId = 1;

        try {
            List<RoomItemResponse> list = userHomeService.listByHomeId(userId, homeId);
            return handleSuccess(list, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    
    @Operation(summary = "대표 집 조회", description = "현재 유저의 대표 집(Primary Home)을 반환합니다.")
    @GetMapping("/address/primary")
    public ResponseEntity<?> getMyPrimaryHome() {
        try {
            Integer userId = 1;
            UserHomeItemResponse res = userHomeService.getPrimaryHome(userId);
            return handleSuccess(res, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    
}
