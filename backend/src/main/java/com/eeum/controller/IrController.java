package com.eeum.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eeum.dto.request.IrRegisterRequest;
import com.eeum.service.IrService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ir")
public class IrController {

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
}
