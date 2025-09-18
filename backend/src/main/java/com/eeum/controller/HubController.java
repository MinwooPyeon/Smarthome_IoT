package com.eeum.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eeum.dto.request.HubRegisterRequest;
import com.eeum.dto.response.HubRegisterResponse;
import com.eeum.service.HubService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/hubs")
@RequiredArgsConstructor
@Tag(name = "Hub API", description = "허브 API")
public class HubController implements ControllerHelper {
	
	private final HubService hubService;

	// 허브 등록
    @Operation(summary = "허브 등록", description = "집에 허브를 등록합니다.")
    @PostMapping("/register")
    public ResponseEntity<?> registerHub(@RequestBody HubRegisterRequest req) {
    	
    	Integer userId = 1;
    	
        try {
            HubRegisterResponse data = hubService.registerHub(userId, req);
            return handleSuccess(data, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
