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
    
    // 조건에 해당하는 device 목록/개수 조회
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
    
    // roomName + deviceName로 deviceId 조회
    @GetMapping("/device-id")
    public ResponseEntity<?> resolveDeviceId(
            @RequestParam(name = "roomName") String roomName,
            @RequestParam(name = "deviceName") String deviceName
    ) {
        try {
            Integer userId = 1;
            
            if (isBlank(roomName) || isBlank(deviceName)) {
                return handleFail(new IllegalArgumentException("roomName과 deviceName은 필수입니다."), HttpStatus.BAD_REQUEST);
            }

            Integer deviceId = deviceService.findDeviceId(userId, roomName.trim(), deviceName.trim());
            if (deviceId == null) {
                return handleFail(new RuntimeException("조건에 해당하는 디바이스가 없습니다."), HttpStatus.NOT_FOUND);
            }

            return handleSuccess(Map.of("deviceId", deviceId), HttpStatus.OK);
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