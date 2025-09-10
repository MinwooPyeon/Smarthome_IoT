package com.eeum.controller;

import com.eeum.dto.request.DeviceStatusRequest;
import com.eeum.dto.request.RegisterDeviceRequest;
import com.eeum.service.DeviceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Tag(name = "Device API", description = "디바이스 API")
public class DeviceController implements ControllerHelper {

    private final DeviceService deviceService;

    // 디바이스 등록
    @Operation(summary = "디바이스 등록", description = "유저가 디바이스를 등록합니다.")
    @PostMapping
    public ResponseEntity<?> registerDevice(@RequestBody RegisterDeviceRequest request) {
        try {
            Integer userId = 1;
            Boolean res = deviceService.registerDevice(userId, request);
  
            return handleSuccess(res, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // 조건에 해당하는 device 목록/개수 조회
    @Operation(
    	    summary = "디바이스 목록 조회",
    	    description = """
    	    사용자가 소속된 집의 디바이스들을 조회합니다.
    	    - 필터(active, type, roomName, deviceName)를 조건으로 검색할 수 있습니다.
    	    - 조건이 없을 경우 전체 목록을 조회합니다.
    	    """
    	)
    @GetMapping
    public ResponseEntity<?> listDevices(
            @RequestParam(name = "active",     required = false) Boolean active,
            @RequestParam(name = "type",       required = false) String type,
            @RequestParam(name = "roomName",   required = false) String roomName,
            @RequestParam(name = "deviceName", required = false) String deviceName
    ) {
        try {
            Integer userId = 1;
            var list = deviceService.findDevices(userId, active, type, roomName, deviceName);
            return handleSuccess(list, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // device 단건 조회
    @Operation(summary = "디바이스 단건 조회", description = "디바이스 정보를 조회합니다.")
    @GetMapping("/{deviceId}")
    public ResponseEntity<?> getDeviceById(@PathVariable("deviceId") Integer deviceId) {
        try {
            Integer userId = 1;
            var dto = deviceService.getDevice(userId, deviceId);
            if (dto == null) {
                return handleFail(new RuntimeException("해당 디바이스가 없습니다."), HttpStatus.NOT_FOUND);
            }
            return handleSuccess(dto, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    
    // 디바이스 상태 수정
    @Operation(
    	    summary = "디바이스 상태 변경",
    	    description = """
    	    device_detail을 부분 갱신합니다.
    	    - 요청 JSON은 **부분 파라미터**만 포함해도 됩니다.
    	    - 예) { "power": true }만 보내면 기존 level 등 다른 필드는 유지됩니다.
    	    """
    	)
    @PutMapping("/{deviceId}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable(name = "deviceId") Integer deviceId,
            @RequestBody DeviceStatusRequest request
    ) {
        try {
        	Integer result = deviceService.updateStatus(deviceId, request);
    	
	        return handleSuccess(Map.of("deviceId", result), HttpStatus.OK);
	    } catch (IllegalArgumentException e) {
	        return handleFail(e, HttpStatus.BAD_REQUEST);
	    } catch (Exception e) {
	        return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
    }
}