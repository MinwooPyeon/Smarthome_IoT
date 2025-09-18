#pragma once

#include <string>
#include <vector>
#include <map>
#include <chrono>
#include <memory>

// 플랫폼별 보안 라이브러리 포함
#ifdef ESP32
#include "mbedtls/sha256.h"
#include "mbedtls/md.h"
#include "mbedtls/aes.h"
#include "mbedtls/gcm.h"
#include "esp_random.h"
#include "esp_log.h"
#elif defined(_WIN32)
#include <windows.h>
#include <wincrypt.h>
#pragma comment(lib, "advapi32.lib")
#pragma comment(lib, "crypt32.lib")
#else
#include <openssl/sha.h>
#include <openssl/hmac.h>
#include <openssl/evp.h>
#include <openssl/rand.h>
#include <openssl/aes.h>
#include <sys/random.h>
#endif

/**
 * @brief 암호화 백엔드 타입
 */
enum class CryptoBackend {
    MBEDTLS,    // ESP32
    OPENSSL,    // Linux/macOS
    CRYPTOAPI   // Windows
};

/**
 * @brief 통합 보안 클래스
 *
 * 플랫폼별 최적화된 보안 기능을 제공합니다.
 * ESP32: mbedtls + 하드웨어 가속
 * Linux: OpenSSL
 * Windows: CryptoAPI
 */
class Security {
private:
    static CryptoBackend backend_;
    static bool initialized_;

    // 플랫폼별 초기화
    static bool initializeBackend();

    // 플랫폼별 구현
    static std::string sha256_mbedtls(const std::string& input);
    static std::string sha256_openssl(const std::string& input);
    static std::string sha256_cryptoapi(const std::string& input);

    static std::string encryptAES_mbedtls(const std::string& plaintext, const std::string& key);
    static std::string encryptAES_openssl(const std::string& plaintext, const std::string& key);
    static std::string encryptAES_cryptoapi(const std::string& plaintext, const std::string& key);

    static std::string decryptAES_mbedtls(const std::string& ciphertext, const std::string& key);
    static std::string decryptAES_openssl(const std::string& ciphertext, const std::string& key);
    static std::string decryptAES_cryptoapi(const std::string& ciphertext, const std::string& key);

    static std::string hmacSha256_mbedtls(const std::string& key, const std::string& message);
    static std::string hmacSha256_openssl(const std::string& key, const std::string& message);
    static std::string hmacSha256_cryptoapi(const std::string& key, const std::string& message);

    static std::vector<uint8_t> generateRandomBytes_mbedtls(size_t length);
    static std::vector<uint8_t> generateRandomBytes_openssl(size_t length);
    static std::vector<uint8_t> generateRandomBytes_cryptoapi(size_t length);

public:
    /**
     * @brief 보안 시스템 초기화
     * @return 초기화 성공 여부
     */
    static bool initialize();

    /**
     * @brief 현재 사용 중인 암호화 백엔드 반환
     * @return 암호화 백엔드
     */
    static CryptoBackend getBackend() { return backend_; }

    /**
     * @brief 보안 시스템 정리
     */
    static void cleanup();
public:
    /**
     * @brief SHA-256 해시 생성 (플랫폼별 최적화)
     * @param input 입력 문자열
     * @return SHA-256 해시 문자열
     */
    static std::string sha256(const std::string& input);

    /**
     * @brief HMAC-SHA256 생성 (플랫폼별 최적화)
     * @param key 비밀 키
     * @param message 메시지
     * @return HMAC-SHA256 해시 문자열
     */
    static std::string hmacSha256(const std::string& key, const std::string& message);

    /**
     * @brief 안전한 랜덤 토큰 생성 (하드웨어 RNG 활용)
     * @param length 토큰 길이
     * @return 랜덤 토큰 문자열
     */
    static std::string generateSecureToken(size_t length = 32);

    /**
     * @brief 토큰 검증 (상수 시간 비교)
     * @param token 검증할 토큰
     * @param expected 예상 토큰
     * @return 검증 성공 여부
     */
    static bool verifyToken(const std::string& token, const std::string& expected);

    /**
     * @brief 시간 기반 토큰 검증 (TOTP)
     * @param token 검증할 토큰
     * @param secret 비밀 키
     * @param window 허용 시간 윈도우 (초)
     * @return 검증 성공 여부
     */
    static bool verifyTimeBasedToken(const std::string& token, const std::string& secret, int window = 30);

    /**
     * @brief AES-256-GCM 암호화 (하드웨어 가속)
     * @param plaintext 평문
     * @param key 암호화 키 (32바이트)
     * @return 암호화된 데이터 (Base64 인코딩)
     */
    static std::string encryptAES(const std::string& plaintext, const std::string& key);

    /**
     * @brief AES-256-GCM 복호화 (하드웨어 가속)
     * @param ciphertext 암호문 (Base64 인코딩)
     * @param key 복호화 키 (32바이트)
     * @return 복호화된 평문
     */
    static std::string decryptAES(const std::string& ciphertext, const std::string& key);

    /**
     * @brief AES-256-CBC 암호화 (호환성용)
     * @param plaintext 평문
     * @param key 암호화 키 (32바이트)
     * @param iv 초기화 벡터 (16바이트)
     * @return 암호화된 데이터 (Base64 인코딩)
     */
    static std::string encryptAES_CBC(const std::string& plaintext, const std::string& key, const std::string& iv);

    /**
     * @brief AES-256-CBC 복호화 (호환성용)
     * @param ciphertext 암호문 (Base64 인코딩)
     * @param key 복호화 키 (32바이트)
     * @param iv 초기화 벡터 (16바이트)
     * @return 복호화된 평문
     */
    static std::string decryptAES_CBC(const std::string& ciphertext, const std::string& key, const std::string& iv);

    /**
     * @brief 비밀번호 해싱 (bcrypt)
     * @param password 평문 비밀번호
     * @param rounds bcrypt 라운드 수
     * @return 해시된 비밀번호
     */
    static std::string hashPassword(const std::string& password, int rounds = 12);

    /**
     * @brief 비밀번호 검증
     * @param password 평문 비밀번호
     * @param hash 해시된 비밀번호
     * @return 검증 성공 여부
     */
    static bool verifyPassword(const std::string& password, const std::string& hash);

    /**
     * @brief 입력 데이터 sanitization
     * @param input 입력 문자열
     * @return sanitized 문자열
     */
    static std::string sanitizeInput(const std::string& input);

    /**
     * @brief SQL 인젝션 방지를 위한 이스케이프
     * @param input 입력 문자열
     * @return 이스케이프된 문자열
     */
    static std::string escapeSql(const std::string& input);

    /**
     * @brief XSS 방지를 위한 HTML 이스케이프
     * @param input 입력 문자열
     * @return 이스케이프된 문자열
     */
    static std::string escapeHtml(const std::string& input);

    /**
     * @brief 파일 경로 검증
     * @param path 파일 경로
     * @return 안전한 경로 여부
     */
    static bool validateFilePath(const std::string& path);

    /**
     * @brief IP 주소 검증
     * @param ip IP 주소
     * @return 유효한 IP 주소 여부
     */
    static bool validateIPAddress(const std::string& ip);

    /**
     * @brief URL 검증
     * @param url URL 문자열
     * @return 유효한 URL 여부
     */
    static bool validateURL(const std::string& url);

    /**
     * @brief 이메일 주소 검증
     * @param email 이메일 주소
     * @return 유효한 이메일 주소 여부
     */
    static bool validateEmail(const std::string& email);

    /**
     * @brief 강력한 비밀번호 검증
     * @param password 비밀번호
     * @return 강력한 비밀번호 여부
     */
    static bool validateStrongPassword(const std::string& password);

    /**
     * @brief 보안 로그 기록
     * @param level 로그 레벨 (INFO, WARN, ERROR)
     * @param message 로그 메시지
     * @param details 추가 세부사항
     */
    static void logSecurityEvent(const std::string& level, const std::string& message, const std::string& details = "");

    /**
     * @brief 보안 정책 검증
     * @param policy 정책 이름
     * @param value 검증할 값
     * @return 정책 준수 여부
     */
    static bool validateSecurityPolicy(const std::string& policy, const std::string& value);

private:
    // 내부 유틸리티 메서드들
    static std::string base64Encode(const std::string& input);
    static std::string base64Decode(const std::string& input);
    static std::vector<uint8_t> generateRandomBytes(size_t length);
    static bool constantTimeCompare(const std::string& a, const std::string& b);
};
