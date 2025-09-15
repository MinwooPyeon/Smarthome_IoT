package com.eeum.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eeum.dto.response.IconResponse;
import com.eeum.service.IconService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Routine Icon API", description = "루틴 아이콘 API")
@RestController
@RequestMapping("/api/icons")
@RequiredArgsConstructor
public class IconController implements ControllerHelper {

    private final IconService iconService;

    @Operation(summary = "루틴 아이콘 목록 조회", description = "루틴에 등록할 아이콘 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<?> getIcons() {
    	try {
            List<IconResponse> icons = iconService.getIcons();
            return handleSuccess(icons);
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
