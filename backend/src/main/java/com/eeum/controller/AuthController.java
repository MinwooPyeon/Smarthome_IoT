package com.eeum.controller;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eeum.dto.request.LoginRequest;
import com.eeum.dto.request.SendEmailRequest;
import com.eeum.dto.request.SignupRequest;
import com.eeum.dto.request.VerifyEmailRequest;
import com.eeum.dto.response.SendEmailResponse;
import com.eeum.dto.response.VerifyEmailResponse;
import com.eeum.service.EmailService;
import com.eeum.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController implements ControllerHelper {
	
	private final EmailService emailService;
    private final UserService userService;

	
	// 인증코드 메일 발송
    @PostMapping("/send")
    public ResponseEntity<SendEmailResponse> send(@RequestBody SendEmailRequest request)
            throws MessagingException, UnsupportedEncodingException {

        int expiresInMinutes = emailService.sendEmail(request.getEmail());
        return ResponseEntity.ok(new SendEmailResponse(true, expiresInMinutes));
    }
	
    // 인증코드 검증
    @PostMapping("/verify-email")
    public ResponseEntity<VerifyEmailResponse> verifyEmail(@RequestBody VerifyEmailRequest request) {
        String email = request.getEmail();
        String code = request.getCode();

        if (!emailService.hasActiveCode(email)) {
            return ResponseEntity.status(410).body(new VerifyEmailResponse(false)); // 만료 또는 미생성
        }

        boolean ok = emailService.verifyEmailCode(email, code);
        if (!ok) {
            return ResponseEntity.badRequest().body(new VerifyEmailResponse(false)); // 불일치
        }

        emailService.invalidateCode(email);

        return ResponseEntity.ok(new VerifyEmailResponse(true));
    }
    
    
    // 회원가입
    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "새로운 사용자를 생성합니다.")
    public ResponseEntity<?> signup(@RequestBody SignupRequest req) {
        try {
            Integer userId = userService.signup(req);
            return handleSuccess(Map.of("userId", userId));
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    
    // 로그인
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "아이디/비밀번호로 로그인합니다. 성공 시 userId 반환")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            Integer userId = userService.login(req);
            return handleSuccess(Map.of("userId", userId));
        } catch (IllegalArgumentException e) {
            return handleFail(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/check-id")
    @Operation(summary = "아이디 중복 확인", description = "이미 가입된 아이디인지 확인합니다.")
    public ResponseEntity<?> checkId(@RequestBody Map<String, String> req) {
        try {
            String email = req.get("email");
            boolean exists = userService.existsByEmail(email);
            return handleSuccess(Map.of("email", email, "exists", exists));
        } catch (Exception e) {
            return handleFail(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
