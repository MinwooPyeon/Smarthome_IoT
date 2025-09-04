package com.eeum.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eeum.dto.request.UpdatePasswordRequest;
import com.eeum.dto.request.UpdateNicknameRequest;
import com.eeum.dto.response.UpdateNicknameResponse;
import com.eeum.security.CustomUserDetails;
import com.eeum.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements ControllerHelper {

    private final UserService userService;

    @PatchMapping("/nickname")
    public ResponseEntity<?> updateNickname(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UpdateNicknameRequest request) {

        try {
            UpdateNicknameResponse response = userService.updateNickname(userDetails.getUserId(), request.getNewNickname());
            return handleSuccess(response);

        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PatchMapping("/password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UpdatePasswordRequest request) {
        try {
            userService.updatePassword(userDetails.getUserId(), request.getNewPassword());
            return handleSuccess(java.util.Map.of("changed", true));
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}