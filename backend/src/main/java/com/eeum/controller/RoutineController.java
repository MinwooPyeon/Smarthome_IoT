package com.eeum.controller;

import com.eeum.dto.request.RoutineCreateRequest;
import com.eeum.dto.request.RoutineUpdateRequest;
import com.eeum.dto.response.RoutineResponse;
import com.eeum.service.RoutineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RoutineController implements ControllerHelper {

    private final RoutineService routineService;

    // 루틴 생성
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
    @GetMapping("/users/{userId}/routines")
    public ResponseEntity<?> list(@PathVariable(name = "userId") Integer userId) {
        try {
            List<RoutineResponse> list = routineService.list(userId);
            return handleSuccess(list, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 루틴 단건 조회
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
