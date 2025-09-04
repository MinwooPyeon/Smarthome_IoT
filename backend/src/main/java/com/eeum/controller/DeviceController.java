package com.eeum.controller;

import com.eeum.dto.request.RegisterDeviceRequest;
import com.eeum.service.DeviceService;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController implements ControllerHelper {

    private final DeviceService deviceService;

    // 디바이스 등록
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
    
    // 조건에 해당하는 기기 목록/개수 조회(조건 없으면 전체)
    @GetMapping
    public ResponseEntity<?> getDevices(
            @RequestParam(name = "roomName", required = false) String roomName,
            @RequestParam(name = "deviceName", required = false) String deviceName,
            @RequestParam(name = "active", required = false) Boolean active,
            @RequestParam(name = "type", required = false) String type
    ) {
        try {
            Integer userId = 1;

            String typeFilter = isBlank(type) ? null : type.trim();
            String roomFilter = isBlank(roomName) ? null : roomName.trim();
            String devFilter  = isBlank(deviceName) ? null : deviceName.trim();

            var result = deviceService.findDevices(userId, active, typeFilter, roomFilter, devFilter);
            return handleSuccess(result, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
    }
    
    
    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}