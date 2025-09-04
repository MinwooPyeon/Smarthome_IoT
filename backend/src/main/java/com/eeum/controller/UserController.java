package com.eeum.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eeum.dto.request.UpdatePasswordRequest;
import com.eeum.dto.request.UpdateNicknameRequest;
import com.eeum.dto.response.UpdateNicknameResponse;
import com.eeum.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements ControllerHelper {

    private final UserService userService;

    @PatchMapping("/{userId}/nickname")
    public ResponseEntity<?> updateNickname(
    		@PathVariable("userId") Integer userId,
            @RequestBody UpdateNicknameRequest request) {

        try {
            UpdateNicknameResponse response = userService.updateNickname(userId, request.getNewNickname());
            return handleSuccess(response);

        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PatchMapping("/{userId}/password")
    public ResponseEntity<?> updatePassword(
    		@PathVariable("userId") Integer userId,
            @RequestBody UpdatePasswordRequest request) {
        try {
            userService.updatePassword(userId, request.getNewPassword());
            return handleSuccess(java.util.Map.of("changed", true));
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}