package com.eeum.controller;

import com.eeum.dto.response.FloorplanResponse;
import com.eeum.entity.Floorplan;
import com.eeum.service.FloorplanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FloorplansController implements ControllerHelper {

    private final FloorplanService floorplanService;

    // 평면도 등록 (수정하기)
    @PutMapping("/users/{userId}/homes/{homeId}/floorplan/{floorplanId}")
    public ResponseEntity<?> selectMyFloorplan(@PathVariable(name = "userId") Integer userId,
                                               @PathVariable(name = "homeId") Integer homeId,
                                               @PathVariable(name = "floorplanId") Integer floorplanId) {
        try {
            floorplanService.selectMyFloorplan(userId, homeId, floorplanId);
            return handleSuccess(Map.of("message", "등록 완료"), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // 특정 homeId에 속한 평면도 목록 조회
    @GetMapping("/homes/{homeId}/floorplans")
    public ResponseEntity<?> getFloorplans(@PathVariable(name = "homeId") Integer homeId) {
        try {
            List<Floorplan> floorplans = floorplanService.findByHome(homeId);
            return handleSuccess(floorplans, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 사용자가 설정한 평면도 리스트 조회 (수정하기)
    @GetMapping("/users/{userId}/homes/{homeId}/floorplan")
    public ResponseEntity<?> getMyCurrentFloorplan(@PathVariable(name = "userId") Integer userId, @PathVariable(name = "homeId") Integer homeId) {
        try {
            List<FloorplanResponse> list = floorplanService.getUserFloorplan(userId, homeId);
            return handleSuccess(list, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
