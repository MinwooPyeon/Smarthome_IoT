package com.eeum.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.eeum.dto.response.AiEnergyReportResponse;
import com.eeum.service.NarrativeReportService;
import com.eeum.service.UserHomeService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reports/ai/energy")
@RequiredArgsConstructor
public class AiReportController {

    private final NarrativeReportService narrativeReportService;
    private final UserHomeService userHomeService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "AI 에너지 요약 리포트",
        description = "사용자의 전체 사용 데이터를 기반으로 분석 결과를 AI가 리포트로 생성하여 반환합니다."
    )
    public ResponseEntity<?> getEnergyAiReport() {
        try {
            Integer userId = 1;

            Integer homeId = userHomeService.getIsPrimaryHomeId(userId)
                .orElseThrow(() -> new IllegalArgumentException("대표 집이 설정되어 있지 않습니다. 대표 집을 먼저 설정해주세요."));

            AiEnergyReportResponse res = narrativeReportService.generate(userId, homeId);
            return ResponseEntity.ok(res);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "INVALID_ARGUMENT",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "INTERNAL_ERROR",
                "message", e.getMessage()
            ));
        }
    }
}
