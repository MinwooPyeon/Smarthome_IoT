# 보안 가이드

## 개요

이 문서는 IR Remote Controller 시스템의 보안 기능과 설정 방법을 설명합니다.

## 보안 기능

### 1. MQTT 통신 보안

#### TLS 암호화
- MQTT 통신에 TLS 암호화 지원
- 클라이언트 인증서 기반 인증
- CA 인증서를 통한 서버 검증

#### 설정 방법
```json
{
    "mqtt_security": {
        "use_tls": true,
        "username_required": true,
        "password_required": true,
        "client_cert_required": true,
        "ca_cert_path": "/etc/ssl/certs/mqtt_ca.crt",
        "client_cert_path": "/etc/ssl/certs/mqtt_client.crt",
        "client_key_path": "/etc/ssl/private/mqtt_client.key"
    }
}
```

#### 사용 예시
```cpp
MqttClient client;
client.setCredentials("username", "password");
client.connectSecure("broker.example.com", 8883,
                     "/path/to/ca.crt",
                     "/path/to/client.crt",
                     "/path/to/client.key");
```

### 2. 시리얼 통신 보안

#### 인증 및 검증
- 토큰 기반 인증
- 명령어 화이트리스트
- 입력 데이터 sanitization
- 속도 제한 (Rate Limiting)

#### 설정 방법
```json
{
    "serial_security": {
        "authentication_required": true,
        "max_message_size_bytes": 1024,
        "rate_limit_messages_per_second": 10,
        "allowed_commands": [
            "ping", "status", "ir_send", "ir_receive"
        ]
    }
}
```

#### 사용 예시
```cpp
SerialController controller;
controller.setAuthenticationToken("secure_token_here");
controller.setMaxMessageSize(1024);
controller.setRateLimit(10);
```

### 3. API 보안

#### 토큰 인증
- API 토큰 기반 인증
- 토큰 검증 및 만료 관리
- CORS 정책 설정

#### 설정 방법
```json
{
    "security": {
        "api_token_required": true,
        "api_token": "CHANGE_THIS_SECRET_TOKEN_IN_PRODUCTION",
        "allowed_origins": [
            "http://localhost:3000",
            "https://yourdomain.com"
        ]
    }
}
```

### 4. 입력 데이터 검증

#### 데이터 Sanitization
- SQL 인젝션 방지
- XSS 공격 방지
- 파일 경로 검증
- 입력 길이 제한

#### 사용 예시
```cpp
std::string userInput = getUserInput();
std::string sanitized = Security::sanitizeInput(userInput);
std::string escaped = Security::escapeHtml(sanitized);
```

### 5. 암호화 및 해싱

#### 비밀번호 보안
- bcrypt 해싱
- 솔트 사용
- 강력한 비밀번호 정책

#### 사용 예시
```cpp
std::string password = "user_password";
std::string hashed = Security::hashPassword(password, 12);
bool isValid = Security::verifyPassword(password, hashed);
```

#### 데이터 암호화
- AES-256-GCM 암호화
- 안전한 키 관리
- Base64 인코딩

```cpp
std::string plaintext = "sensitive_data";
std::string encrypted = Security::encryptAES(plaintext, "secret_key");
std::string decrypted = Security::decryptAES(encrypted, "secret_key");
```

## 보안 설정 파일

### security_config.json
```json
{
    "security": {
        "api_token_required": true,
        "tls_enabled": false,
        "rate_limit_enabled": true,
        "input_validation_enabled": true,
        "security_logging_enabled": true
    },
    "password_policy": {
        "min_length": 8,
        "require_uppercase": true,
        "require_lowercase": true,
        "require_numbers": true,
        "require_special_chars": true
    }
}
```

## 보안 모범 사례

### 1. 토큰 관리
- 강력한 토큰 생성
- 정기적인 토큰 교체
- 안전한 토큰 저장

### 2. 네트워크 보안
- TLS 암호화 사용
- 방화벽 설정
- 불필요한 포트 차단

### 3. 로깅 및 모니터링
- 보안 이벤트 로깅
- 실패한 인증 시도 추적
- 정기적인 로그 검토

### 4. 시스템 보안
- 정기적인 보안 업데이트
- 최소 권한 원칙
- 보안 정책 준수

## 보안 체크리스트

### 배포 전 확인사항
- [ ] 기본 토큰 변경
- [ ] TLS 인증서 설정
- [ ] 방화벽 규칙 확인
- [ ] 로그 디렉토리 권한 설정
- [ ] 보안 설정 파일 권한 확인

### 운영 중 확인사항
- [ ] 정기적인 로그 검토
- [ ] 보안 업데이트 적용
- [ ] 토큰 교체
- [ ] 인증서 만료 확인
- [ ] 접근 로그 모니터링

## 보안 이벤트 대응

### 일반적인 보안 이벤트
1. **인증 실패**: 로그 확인, IP 차단 고려
2. **비정상적인 접근**: 패턴 분석, 추가 모니터링
3. **시스템 침해 의심**: 즉시 격리, 포렌식 분석

### 대응 절차
1. 이벤트 확인 및 분류
2. 영향도 평가
3. 대응 조치 실행
4. 사후 분석 및 개선

## 연락처

보안 관련 문의사항이나 취약점 신고는 다음으로 연락해주세요:
- 이메일: security@yourdomain.com
- 보안팀: security-team@yourdomain.com

## 라이선스

이 보안 가이드는 MIT 라이선스 하에 배포됩니다.
