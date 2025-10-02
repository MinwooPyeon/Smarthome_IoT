package com.eeum.service;

import java.io.UnsupportedEncodingException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.eeum.service.code.VerificationCodeStore;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

//    private final StringRedisTemplate redisTemplate;
	private final VerificationCodeStore codeStore;
    private final MailSenderService mailSenderService;

    private static final int CODE_LENGTH = 6;
    private static final int EXPIRES_MINUTES = 5;
    private static final String KEY_PREFIX = "email:code:";

    private static final Pattern EMAIL_REGEX = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern CODE_REGEX = Pattern.compile("^\\d{6}$");

    /** 인증코드 발송: 코드 생성 → Redis 저장 → 메일 발송 → 유효분 반환 */
    public int sendEmail(String email) throws MessagingException, UnsupportedEncodingException {
        validateEmail(email);

        String code = generateCode();

        // Redis 저장
//        redisTemplate.opsForValue().set(key(email), code, EXPIRES_MINUTES, TimeUnit.MINUTES);
        codeStore.save(email, code, EXPIRES_MINUTES);
        
        String subject = "[EEUM] 이메일 인증코드";
        String content = """
                <p>요청하신 이메일 인증코드를 안내드립니다.</p>
                <p style="font-size: 18px; font-weight: bold;">인증코드: %s</p>
                <p>인증코드는 <b>%d분</b>간 유효합니다.</p>
                """.formatted(code, EXPIRES_MINUTES);

        mailSenderService.sendMail(email, subject, content);
        return EXPIRES_MINUTES;
    }

    /** 코드 존재 여부(만료/미생성 구분 목적) */
    public boolean hasActiveCode(String email) {
//        return redisTemplate.hasKey(key(email));
        return codeStore.exists(email);
    }

    /** 코드 검증 (일치/불일치) */
    public boolean verifyEmailCode(String email, String code) {
        validateEmail(email);
        validateCode(code);

//        String saved = redisTemplate.opsForValue().get(key(email));
        String saved = codeStore.get(email);
        return saved != null && saved.equals(code);
    }

    /** 검증 성공 후 무효화 */
    public void invalidateCode(String email) {
//        redisTemplate.delete(key(email));
        codeStore.delete(email);
    }

    private String key(String email) {
        return KEY_PREFIX + email;
    }

    private void validateEmail(String email) {
        if (email == null || !EMAIL_REGEX.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    private void validateCode(String code) {
        if (code == null || !CODE_REGEX.matcher(code).matches()) {
            throw new IllegalArgumentException("Invalid code format");
        }
    }

    private String generateCode() {
        Random random = new Random();
        int n = (int) Math.pow(10, CODE_LENGTH - 1);
        int number = n + random.nextInt(9 * n);
        return String.valueOf(number);
    }
}
