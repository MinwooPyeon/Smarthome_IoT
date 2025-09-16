package com.eeum.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eeum.dto.request.UserImageUpdateRequest;
import com.eeum.dto.request.PasswordUpdateRequest;
import com.eeum.dto.request.UpdateNicknameRequest;
import com.eeum.dto.response.UpdateNicknameResponse;
import com.eeum.dto.response.UserResponse;
import com.eeum.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "User API", description = "사용자 API")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController implements ControllerHelper {

    private final UserService userService;
    
    private static final Integer TEMP_USER_ID = 1;

    
    // 닉네임 변경
    @Operation(summary = "닉네임 변경", description = "로그인한 사용자의 닉네임을 변경합니다.")
    @PatchMapping("/nickname")
    public ResponseEntity<?> updateNickname(@RequestBody UpdateNicknameRequest request) {

        try {
            UpdateNicknameResponse response = userService.updateNickname(TEMP_USER_ID, request.getNewNickname());
            return handleSuccess(response);

        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    
    // 유저 정보 조회
    @Operation(summary = "내 정보 조회", description = "로그인한 사용자의 프로필 정보를 조회합니다.")
    @GetMapping
    public ResponseEntity<?> getUserInfo() {
        try {
            UserResponse user = userService.getUserInfo(TEMP_USER_ID);
            return handleSuccess(user);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // 유저 이미지 변경
    @Operation(summary = "프로필 이미지 변경", description = "로그인한 사용자의 프로필 이미지를 변경합니다.")
    @PutMapping("/image")
    public ResponseEntity<?> updateUserImage(@RequestBody UserImageUpdateRequest req) {
        try {
            userService.updateUserImage(TEMP_USER_ID, req);
            return handleSuccess(Map.of("message", "이미지 변경 완료"));
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 유저 비밀번호 변경
    @Operation(summary = "비밀번호 변경", description = "로그인한 사용자의 비밀번호를 변경합니다.")
    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(@RequestBody PasswordUpdateRequest req) {
        try {
            userService.updatePassword(TEMP_USER_ID, req);
            return handleSuccess(Map.of("message", "비밀번호 변경 완료"));
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 회원탈퇴
//    @Operation(summary = "회원 탈퇴", description = "로그인한 사용자를 탈퇴 처리합니다.")
//    @DeleteMapping
//    public ResponseEntity<?> deleteUser() {
//        try {
//            userService.deleteUser(TEMP_USER_ID);
//            return handleSuccess(Map.of("message", "회원 탈퇴 완료"));
//        } catch (Exception e) {
//            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
}