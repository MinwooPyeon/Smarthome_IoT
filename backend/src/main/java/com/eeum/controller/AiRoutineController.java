package com.eeum.controller;

import com.eeum.entity.AiRoutine;
import com.eeum.service.AiRoutineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai-routines")
@RequiredArgsConstructor
@Tag(name = "AI Routine", description = "AI 루틴 관리 API")
public class AiRoutineController {

    private final AiRoutineService service;

    @Operation(
            summary = "AI 루틴 전체 조회",
            description = "모든 AI 루틴 목록을 조회합니다."
    )
    @GetMapping
    public List<AiRoutine> getAll() {
        return service.getAll();
    }
}
