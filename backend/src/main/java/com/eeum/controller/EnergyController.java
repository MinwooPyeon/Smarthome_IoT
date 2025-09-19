package com.eeum.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.eeum.dto.response.EnergySeriesResponse;
import com.eeum.dto.response.EnergyTypeResponse;
import com.eeum.service.EnergyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/energy")
@RequiredArgsConstructor
@Tag(name = "Energy API", description = "전력 사용량 API")
public class EnergyController implements ControllerHelper {

    private final EnergyService energyService;

    // 기간별 전체 사용량
    @GetMapping("/series")
    @Operation(
        summary = "기간별 전체 사용량",
        description = """
            일간/주간/월간/연간 사용 전력량(kWh)을 시계열로 반환합니다.
            - range: day(시간별), week/month(일별), year(월별)
            - date: 기준 날짜(해당 일/주/월/년을 지정), 미지정 시 오늘
            """,
        parameters = {
            @Parameter(name = "homeId", required = true, description = "집 ID", example = "1"),
            @Parameter(name = "range", required = false, description = "집계 범위 (day|week|month|year)", example = "day"),
            @Parameter(name = "date",  required = false, description = "기준 날짜(YYYY-MM-DD)", example = "2025-09-19")
        }
    )
    public ResponseEntity<?> series(
            @RequestParam(name = "homeId") Integer homeId,
            @RequestParam(name = "range",  defaultValue = "day") String range,
            @RequestParam(name = "date",   required = false) String dateIso
    ) {
        Integer userId = 1;
        try {
            EnergySeriesResponse data = energyService.getUsageSeries(userId, homeId, range, dateIso);
            return handleSuccess(data, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 기간별 타입별 사용량
    @GetMapping("/by-type")
    @Operation(
        summary = "기간별 타입별 사용량",
        description = """
            일간/주간/월간/연간 타입별 kWh/점유율을 반환합니다.
            """,
        parameters = {
            @Parameter(name = "homeId", required = true, description = "집 ID", example = "1"),
            @Parameter(name = "range", required = false, description = "집계 범위 (day|week|month|year)", example = "day"),
            @Parameter(name = "date",  required = false, description = "기준 날짜(YYYY-MM-DD)", example = "2025-09-19")
        }
    )
    public ResponseEntity<?> byType(
            @RequestParam(name = "homeId") Integer homeId,
            @RequestParam(name = "range",  defaultValue = "day") String range,
            @RequestParam(name = "date",   required = false) String dateIso
    ) {
        Integer userId = 1;
        try {
            EnergyTypeResponse data = energyService.getUsageByType(userId, homeId, range, dateIso);
            return handleSuccess(data, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
