package com.eeum.controller;

import com.eeum.dto.request.FloorplanRequest;
import com.eeum.service.FloorplanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/floorplans")
@RequiredArgsConstructor
public class FloorplansController implements ControllerHelper {

    private final FloorplanService floorplanService;

    // 평면도 추가
    @PostMapping
    public ResponseEntity<?> addFloorplan(@RequestBody FloorplanRequest req) {
        try {
            Integer floorplanId = floorplanService.addFloorplan(req);
            return handleSuccess(Map.of("floorplanId", floorplanId), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
