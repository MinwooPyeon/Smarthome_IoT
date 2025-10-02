package com.eeum.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eeum.dto.request.IrRegisterRequest;
import com.eeum.dto.response.DeviceLogItemResponse;
import com.eeum.service.IrService;
import com.eeum.service.DeviceService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ir")
public class IrController implements ControllerHelper {

    private final DeviceService deviceService;
    private final IrService irService;
    
    // 리모컨 버튼 등록
    @Operation(summary = "리모컨 버튼 등록", description = "가전 제품에 사용되고 있는 리모컨 신호를 등록합니다.")
    @PostMapping("/request")
    public ResponseEntity<String> requestIr(@RequestBody IrRegisterRequest request) {
        try {
            irService.requestIrLearning(request);
            return ResponseEntity.ok("IR 학습 요청 완료");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("요청 실패: " + e.getMessage());
        }
    }
	
	
    @GetMapping("/logs")
    @Operation(summary = "이벤트 로그 조회", description = "기기 제어 로그를 반환합니다.")
    public ResponseEntity<?> listLogs(
            @RequestParam(name = "homeId") Integer homeId,
            @RequestParam(name = "limit", defaultValue = "50") Integer limit
    ) {
        Integer userId = 1; 
        try {
        	List<DeviceLogItemResponse> data = deviceService.listRecentLogs(userId, homeId, limit);
            return handleSuccess(data, HttpStatus
            		.OK);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
