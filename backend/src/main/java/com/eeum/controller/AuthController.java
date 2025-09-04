package com.eeum.controller;

import java.io.UnsupportedEncodingException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eeum.dto.request.SendEmailRequest;
import com.eeum.dto.request.VerifyEmailRequest;
import com.eeum.dto.response.SendEmailResponse;
import com.eeum.dto.response.VerifyEmailResponse;
import com.eeum.service.EmailService;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
	
	private final EmailService emailService;
	
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
}
