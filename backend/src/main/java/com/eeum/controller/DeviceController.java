package com.eeum.controller;

import com.eeum.dto.request.DeviceStatusRequest;
import com.eeum.dto.request.RegisterDeviceRequest;
import com.eeum.dto.request.UpdateDeviceLocationRequest;
import com.eeum.dto.response.DeviceLocationResponse;
import com.eeum.service.DeviceService;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
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
            @RequestParam(name = "power",     required = false) Boolean power,
            @RequestParam(name = "type",       required = false) String type,
            @RequestParam(name = "roomName",   required = false) String roomName,
            @RequestParam(name = "deviceName", required = false) String deviceName
    ) {
        try {
            Integer userId = 1;
            var list = deviceService.findDevices(userId, power, type, roomName, deviceName);
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
    	    - power, temperature, level만 입력 가능합니다.
    	    """
    	)
    @PutMapping("/{deviceId}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable(name = "deviceId") Integer deviceId,
            @RequestBody DeviceStatusRequest request
    ) {
        try {
            log.debug("[API] PUT /devices/{}/status body={}", deviceId, safeJson(request.getDeviceDetail()));
        	Integer result = deviceService.updateStatus(deviceId, request);
    	
	        return handleSuccess(Map.of("deviceId", result), HttpStatus.OK);
	    } catch (IllegalArgumentException e) {
	        return handleFail(e, HttpStatus.BAD_REQUEST);
	    } catch (Exception e) {
	        return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
    }
    
    @Operation(
            summary = "디바이스 삭제",
            description = "사용자가 소유한 디바이스를 삭제합니다. 관련된 device_positions, command, routine_detail 레코드도 함께 삭제됩니다.")
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<?> deleteDevice(@PathVariable("deviceId") Integer deviceId) {
        try {
            Integer userId = 1;
            deviceService.deleteDevice(userId, deviceId);
            return handleSuccess(Map.of("deviceId", deviceId, "deleted", true), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    
    @PutMapping("/{deviceId}/location")
    @Operation(summary = "디바이스 위치 수정", description = "평면도에서 디바이스 위치를 수정합니다.")
    public ResponseEntity<?> updateLocation(@PathVariable(name = "deviceId") Integer deviceId,
                                            @RequestBody UpdateDeviceLocationRequest request) {
        try {
            Integer userId = 1;
            Integer resultId = deviceService.updateLocation(userId, deviceId, request);
            return handleSuccess(java.util.Map.of("deviceId", resultId, "updated", true), HttpStatus.OK);
        } catch (java.util.NoSuchElementException e) {
            return handleFail(e, HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    
    @Operation(summary = "디바이스 위치 목록 조회", description = "사용자가 현재 선택한 홈에 속한 모든 디바이스의 위치를 반환합니다.")
        @GetMapping("/locations")
        public ResponseEntity<?> listDeviceLocations(
                @RequestParam("homeId") Integer homeId
        ) {
            try {
                Integer userId = 1;
                List<DeviceLocationResponse> list = deviceService.listDeviceLocations(userId, homeId);
                return handleSuccess(list, HttpStatus.OK);
            } catch (IllegalArgumentException e) {
                return handleFail(e, HttpStatus.BAD_REQUEST);
            } catch (Exception e) {
                return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    private String safeJson(Object o) {
    	  try { return new ObjectMapper().writeValueAsString(o); }
    	  catch (Exception e) { return String.valueOf(o); }
    	}
}