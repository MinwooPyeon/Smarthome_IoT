package com.eeum.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eeum.dto.request.RoutineCreateRequest;
import com.eeum.dto.request.RoutineUpdateRequest;
import com.eeum.dto.response.RoutineResponse;
import com.eeum.service.RoutineService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Routine API", description = "루틴 관리 API")
public class RoutineController implements ControllerHelper {

    private final RoutineService routineService;

    // 루틴 생성
    @Operation(summary = "루틴 생성", description = "유저의 루틴을 생성합니다.")
    @PostMapping("/routines")
    public ResponseEntity<?> create(@RequestBody RoutineCreateRequest req) {
    	
    	Integer userId = 1;
        try {
            Integer id = routineService.create(userId, req);
            return handleSuccess(Map.of("routineId", id), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 루틴 수정
    @Operation(summary = "루틴 수정", description = "유저의 루틴을 수정합니다.")
    @PutMapping("/routines/{routineId}")
    public ResponseEntity<?> update(@PathVariable(name = "routineId") Integer routineId,
                                    @RequestBody RoutineUpdateRequest req) {
    	Integer userId = 1;
        try {
            routineService.update(userId, routineId, req);
            return handleSuccess(Map.of("message", "수정 완료"), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 루틴 삭제
    @Operation(summary = "루틴 삭제", description = "유저의 루틴을 삭제합니다.")
    @DeleteMapping("/routines/{routineId}")
    public ResponseEntity<?> delete(@PathVariable(name = "routineId") Integer routineId) {
    	Integer userId = 1;
    	
        try {
            routineService.delete(userId, routineId);
            return handleSuccess(Map.of("message", "삭제 완료"), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 루틴 전체 조회
    @Operation(
    	    summary = "루틴 전체 조회",
    	    description = """
    	                  유저의 모든 루틴을 조회합니다.
    	                  - 요일 필터: 비트마스크(월=1<<0,…, 일=1<<6). 예) 월=1, 화=2, 월+수=1|4=5
    	                  - 필터 없으면 전체 반환
    	                  """
    	)
    	@GetMapping("/routines")
    	public ResponseEntity<?> list(
    	        @Parameter(description = "요일 비트마스크(월=1<<0,…,일=1<<6). 예: 월=1, 화=2, 월+수=5", example = "5")
    	        @RequestParam(name = "weekday", required = false) Integer weekday
    	) {
    	Integer userId = 1;
    	
    	    try {
    	        List<RoutineResponse> list = routineService.list(userId, weekday);
    	        return handleSuccess(list, HttpStatus.OK);
    	    } catch (IllegalArgumentException e) {
    	        return handleFail(e, HttpStatus.BAD_REQUEST);
    	    } catch (Exception e) {
    	        return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
    	    }
    	}

    // 루틴 단건 조회
    @Operation(summary = "루틴 단건 조회", description = "유저의 루틴을 조회합니다.")
    @GetMapping("/routines/{routineId}")
    public ResponseEntity<?> get(@PathVariable(name = "routineId") Integer routineId) {
    	
    	Integer userId = 1;
        try {
            RoutineResponse res = routineService.get(userId, routineId);
            return handleSuccess(res, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // 루틴 실행 테스트
    @Operation(summary = "루틴 즉시 실행", description = "해당 루틴을 즉시 실행합니다.")
    @PostMapping("/routines/{routineId}/execute")
    public ResponseEntity<?> execute(@PathVariable(name = "routineId") Integer routineId) {
        Integer userId = 1;
        try {
            routineService.executeRoutine(userId, routineId); // ★ RoutineService 실행 진입점
            return handleSuccess(Map.of("message", "루틴 실행 요청 완료", "routineId", routineId), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
