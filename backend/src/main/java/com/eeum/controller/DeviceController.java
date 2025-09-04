package com.eeum.controller;

import com.eeum.dto.request.RegisterDeviceRequest;
import com.eeum.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class DeviceController implements ControllerHelper {

    private final DeviceService deviceService;

    @PostMapping("/{userId}/devices")
    public ResponseEntity<?> registerDevice(
            @PathVariable("userId") Integer userId,
            @RequestBody RegisterDeviceRequest request) {
        try {
        	Boolean res = deviceService.registerDevice(userId, request);
            return handleSuccess(res, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}