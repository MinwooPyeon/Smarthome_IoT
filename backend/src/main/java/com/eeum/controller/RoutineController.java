package com.eeum.controller;

import com.eeum.dto.request.RoutineCreateRequest;
import com.eeum.dto.request.RoutineUpdateRequest;
import com.eeum.dto.response.RoutineResponse;
import com.eeum.service.RoutineService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Routine API", description = "루틴 관리 API")
public class RoutineController implements ControllerHelper {

    private final RoutineService routineService;

    // 루틴 생성
    @Operation(summary = "루틴 생성", description = "유저의 루틴을 생성합니다.")
    @PostMapping("/users/{userId}/routines")
    public ResponseEntity<?> create(@PathVariable(name = "userId") Integer userId,
                                    @RequestBody RoutineCreateRequest req) {
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
    @PutMapping("/users/{userId}/routines/{routineId}")
    public ResponseEntity<?> update(@PathVariable(name = "userId") Integer userId,
                                    @PathVariable(name = "routineId") Integer routineId,
                                    @RequestBody RoutineUpdateRequest req) {
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
    @DeleteMapping("/users/{userId}/routines/{routineId}")
    public ResponseEntity<?> delete(@PathVariable(name = "userId") Integer userId,
                                    @PathVariable(name = "routineId") Integer routineId) {
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
    	@GetMapping("/users/{userId}/routines")
    	public ResponseEntity<?> list(
    	        @PathVariable(name = "userId") Integer userId,
    	        @Parameter(description = "요일 비트마스크(월=1<<0,…,일=1<<6). 예: 월=1, 화=2, 월+수=5", example = "5")
    	        @RequestParam(name = "mask", required = false) Integer mask
    	) {
    	    try {
    	        List<RoutineResponse> list = routineService.list(userId, mask);
    	        return handleSuccess(list, HttpStatus.OK);
    	    } catch (IllegalArgumentException e) {
    	        return handleFail(e, HttpStatus.BAD_REQUEST);
    	    } catch (Exception e) {
    	        return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
    	    }
    	}

    // 루틴 단건 조회
    @Operation(summary = "루틴 단건 조회", description = "유저의 루틴을 조회합니다.")
    @GetMapping("/users/{userId}/routines/{routineId}")
    public ResponseEntity<?> get(@PathVariable(name = "userId") Integer userId,
                                 @PathVariable(name = "routineId") Integer routineId) {
        try {
            RoutineResponse res = routineService.get(userId, routineId);
            return handleSuccess(res, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
