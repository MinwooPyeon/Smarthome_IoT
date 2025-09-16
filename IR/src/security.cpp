#include "core/security.h"
#include <iostream>
#include <sstream>
#include <iomanip>
#include <random>
#include <algorithm>
#include <regex>
#include <fstream>
#include <ctime>

#ifdef _WIN32
#include <windows.h>
#include <wincrypt.h>
#pragma comment(lib, "advapi32.lib")
#pragma comment(lib, "crypt32.lib")
#elif defined(ESP_PLATFORM)
// ESP32 환경에서는 ESP-IDF crypto 라이브러리 사용
#include "esp_log.h"
#include "mbedtls/sha256.h"
#include "mbedtls/md.h"
#include "mbedtls/aes.h"
#include "esp_random.h"
#else
#include <openssl/sha.h>
#include <openssl/hmac.h>
#include <openssl/evp.h>
#include <openssl/rand.h>
#include <openssl/aes.h>
#include <openssl/err.h>
#include <unistd.h>
#include <sys/random.h>
#endif

// 로그 함수 (실제 구현에서는 적절한 로깅 시스템 사용)
void Security::logSecurityEvent(const std::string& level, const std::string& message, const std::string& details) {
    auto now = std::chrono::system_clock::now();
    auto time_t = std::chrono::system_clock::to_time_t(now);

    std::cout << "[" << std::put_time(std::localtime(&time_t), "%Y-%m-%d %H:%M:%S") << "] "
              << "[SECURITY-" << level << "] " << message;
    if (!details.empty()) {
        std::cout << " - " << details;
    }
    std::cout << std::endl;
}

std::string Security::sha256(const std::string& input) {
#ifdef _WIN32
    // Windows CryptoAPI 사용
    HCRYPTPROV hProv = 0;
    HCRYPTHASH hHash = 0;
    BYTE rgbHash[32];
    DWORD cbHash = 32;

    if (!CryptAcquireContext(&hProv, NULL, NULL, PROV_RSA_AES, CRYPT_VERIFYCONTEXT)) {
        logSecurityEvent("ERROR", "CryptAcquireContext failed");
        return "";
    }

    if (!CryptCreateHash(hProv, CALG_SHA_256, 0, 0, &hHash)) {
        logSecurityEvent("ERROR", "CryptCreateHash failed");
        CryptReleaseContext(hProv, 0);
        return "";
    }

    if (!CryptHashData(hHash, (BYTE*)input.c_str(), input.length(), 0)) {
        logSecurityEvent("ERROR", "CryptHashData failed");
        CryptDestroyHash(hHash);
        CryptReleaseContext(hProv, 0);
        return "";
    }

    if (!CryptGetHashParam(hHash, HP_HASHVAL, rgbHash, &cbHash, 0)) {
        logSecurityEvent("ERROR", "CryptGetHashParam failed");
        CryptDestroyHash(hHash);
        CryptReleaseContext(hProv, 0);
        return "";
    }

    CryptDestroyHash(hHash);
    CryptReleaseContext(hProv, 0);

    std::stringstream ss;
    for (int i = 0; i < 32; i++) {
        ss << std::hex << std::setw(2) << std::setfill('0') << (int)rgbHash[i];
    }
    return ss.str();
#elif defined(ESP_PLATFORM)
    // ESP32 환경에서는 ESP-IDF crypto 라이브러리 사용
    unsigned char hash[32];
    mbedtls_sha256_context sha256_ctx;
    mbedtls_sha256_init(&sha256_ctx);
    mbedtls_sha256_starts(&sha256_ctx, 0);
    mbedtls_sha256_update(&sha256_ctx, (const unsigned char*)input.c_str(), input.length());
    mbedtls_sha256_finish(&sha256_ctx, hash);
    mbedtls_sha256_free(&sha256_ctx);

    std::stringstream ss;
    for (int i = 0; i < 32; i++) {
        ss << std::hex << std::setw(2) << std::setfill('0') << (int)hash[i];
    }
    return ss.str();
#else
    // OpenSSL 사용
    unsigned char hash[SHA256_DIGEST_LENGTH];
    SHA256_CTX sha256;
    SHA256_Init(&sha256);
    SHA256_Update(&sha256, input.c_str(), input.length());
    SHA256_Final(hash, &sha256);

    std::stringstream ss;
    for (int i = 0; i < SHA256_DIGEST_LENGTH; i++) {
        ss << std::hex << std::setw(2) << std::setfill('0') << (int)hash[i];
    }
    return ss.str();
#endif
}

std::string Security::hmacSha256(const std::string& key, const std::string& message) {
#ifdef _WIN32
    // Windows에서는 간단한 구현 (실제로는 CryptoAPI 사용 권장)
    std::string combined = key + message;
    return sha256(combined);
#elif defined(ESP_PLATFORM)
    // ESP32 환경에서는 mbedtls 사용
    unsigned char result[32];
    const mbedtls_md_info_t* md_info = mbedtls_md_info_from_type(MBEDTLS_MD_SHA256);
    mbedtls_md_context_t md_ctx;

    mbedtls_md_init(&md_ctx);
    mbedtls_md_setup(&md_ctx, md_info, 1);
    mbedtls_md_hmac_starts(&md_ctx, (const unsigned char*)key.c_str(), key.length());
    mbedtls_md_hmac_update(&md_ctx, (const unsigned char*)message.c_str(), message.length());
    mbedtls_md_hmac_finish(&md_ctx, result);
    mbedtls_md_free(&md_ctx);

    std::stringstream ss;
    for (int i = 0; i < 32; i++) {
        ss << std::hex << std::setw(2) << std::setfill('0') << (int)result[i];
    }
    return ss.str();
#else
    unsigned char* result;
    unsigned int len = 32;

    result = HMAC(EVP_sha256(), key.c_str(), key.length(),
                  (unsigned char*)message.c_str(), message.length(), NULL, &len);

    if (result == NULL) {
        logSecurityEvent("ERROR", "HMAC-SHA256 failed");
        return "";
    }

    std::stringstream ss;
    for (unsigned int i = 0; i < len; i++) {
        ss << std::hex << std::setw(2) << std::setfill('0') << (int)result[i];
    }
    return ss.str();
#endif
}

std::string Security::generateSecureToken(size_t length) {
    std::string token;
    token.reserve(length);

    const std::string chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<> dis(0, chars.size() - 1);

    for (size_t i = 0; i < length; i++) {
        token += chars[dis(gen)];
    }

    return token;
}

bool Security::verifyToken(const std::string& token, const std::string& expected) {
    return constantTimeCompare(token, expected);
}

bool Security::verifyTimeBasedToken(const std::string& token, const std::string& secret, int window) {
    auto now = std::chrono::system_clock::now();
    auto time_t = std::chrono::system_clock::to_time_t(now);
    uint64_t time_step = time_t / window;

    // 현재 시간 윈도우와 이전/다음 윈도우 검증
    for (int i = -1; i <= 1; i++) {
        uint64_t test_time = time_step + i;
        std::string test_token = hmacSha256(secret, std::to_string(test_time));
        if (constantTimeCompare(token, test_token)) {
            return true;
        }
    }

    return false;
}

std::string Security::encryptAES(const std::string& plaintext, const std::string& key) {
    // 간단한 XOR 암호화 (실제로는 AES-GCM 사용 권장)
    std::string ciphertext = plaintext;
    size_t key_len = key.length();

    for (size_t i = 0; i < ciphertext.length(); i++) {
        ciphertext[i] ^= key[i % key_len];
    }

    return base64Encode(ciphertext);
}

std::string Security::decryptAES(const std::string& ciphertext, const std::string& key) {
    std::string decoded = base64Decode(ciphertext);
    size_t key_len = key.length();

    for (size_t i = 0; i < decoded.length(); i++) {
        decoded[i] ^= key[i % key_len];
    }

    return decoded;
}

std::string Security::hashPassword(const std::string& password, int rounds) {
    // 간단한 해싱 (실제로는 bcrypt 사용 권장)
    std::string salt = generateSecureToken(16);
    std::string hash = sha256(password + salt);

    return "$2a$" + std::to_string(rounds) + "$" + salt + hash;
}

bool Security::verifyPassword(const std::string& password, const std::string& hash) {
    // 간단한 검증 (실제로는 bcrypt 검증 사용 권장)
    if (hash.length() < 10) return false;

    std::string salt = hash.substr(7, 16);
    std::string expected = sha256(password + salt);
    std::string stored = hash.substr(23);

    return constantTimeCompare(expected, stored);
}

std::string Security::sanitizeInput(const std::string& input) {
    std::string sanitized = input;

    // NULL 바이트 제거
    sanitized.erase(std::remove(sanitized.begin(), sanitized.end(), '\0'), sanitized.end());

    // 제어 문자 제거 (탭, 개행, 캐리지 리턴 제외)
    sanitized.erase(std::remove_if(sanitized.begin(), sanitized.end(),
        [](char c) {
            return c < 32 && c != '\t' && c != '\n' && c != '\r';
        }), sanitized.end());

    // 최대 길이 제한
    if (sanitized.length() > 10000) {
        sanitized = sanitized.substr(0, 10000);
    }

    return sanitized;
}

std::string Security::escapeSql(const std::string& input) {
    std::string escaped = input;

    // SQL 인젝션 방지를 위한 이스케이프
    size_t pos = 0;
    while ((pos = escaped.find("'", pos)) != std::string::npos) {
        escaped.replace(pos, 1, "''");
        pos += 2;
    }

    return escaped;
}

std::string Security::escapeHtml(const std::string& input) {
    std::string escaped = input;

    // HTML 이스케이프
    size_t pos = 0;
    while ((pos = escaped.find("&", pos)) != std::string::npos) {
        escaped.replace(pos, 1, "&amp;");
        pos += 5;
    }

    pos = 0;
    while ((pos = escaped.find("<", pos)) != std::string::npos) {
        escaped.replace(pos, 1, "&lt;");
        pos += 4;
    }

    pos = 0;
    while ((pos = escaped.find(">", pos)) != std::string::npos) {
        escaped.replace(pos, 1, "&gt;");
        pos += 4;
    }

    pos = 0;
    while ((pos = escaped.find("\"", pos)) != std::string::npos) {
        escaped.replace(pos, 1, "&quot;");
        pos += 6;
    }

    return escaped;
}

bool Security::validateFilePath(const std::string& path) {
    // 경로 순회 공격 방지
    if (path.find("..") != std::string::npos) {
        return false;
    }

    // 절대 경로 방지
    if (path.length() > 0 && (path[0] == '/' || path[0] == '\\')) {
        return false;
    }

    // 금지된 문자 검사
    const std::string forbidden = "<>:\"|?*";
    for (char c : forbidden) {
        if (path.find(c) != std::string::npos) {
            return false;
        }
    }

    return true;
}

bool Security::validateIPAddress(const std::string& ip) {
    std::regex ipv4_regex("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
    return std::regex_match(ip, ipv4_regex);
}

bool Security::validateURL(const std::string& url) {
    std::regex url_regex("^https?://[a-zA-Z0-9.-]+(\\.[a-zA-Z]{2,})?(/.*)?$");
    return std::regex_match(url, url_regex);
}

bool Security::validateEmail(const std::string& email) {
    std::regex email_regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    return std::regex_match(email, email_regex);
}

bool Security::validateStrongPassword(const std::string& password) {
    if (password.length() < 8) return false;

    bool has_upper = false, has_lower = false, has_digit = false, has_special = false;

    for (char c : password) {
        if (std::isupper(c)) has_upper = true;
        else if (std::islower(c)) has_lower = true;
        else if (std::isdigit(c)) has_digit = true;
        else if (!std::isalnum(c)) has_special = true;
    }

    return has_upper && has_lower && has_digit && has_special;
}

bool Security::validateSecurityPolicy(const std::string& policy, const std::string& value) {
    if (policy == "password") {
        return validateStrongPassword(value);
    } else if (policy == "email") {
        return validateEmail(value);
    } else if (policy == "ip") {
        return validateIPAddress(value);
    } else if (policy == "url") {
        return validateURL(value);
    } else if (policy == "filepath") {
        return validateFilePath(value);
    }

    return false;
}

// 내부 유틸리티 메서드들
std::string Security::base64Encode(const std::string& input) {
    const std::string chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    std::string result;
    int val = 0, valb = -6;

    for (unsigned char c : input) {
        val = (val << 8) + c;
        valb += 8;
        while (valb >= 0) {
            result.push_back(chars[(val >> valb) & 0x3F]);
            valb -= 6;
        }
    }

    if (valb > -6) {
        result.push_back(chars[((val << 8) >> (valb + 8)) & 0x3F]);
    }

    while (result.size() % 4) {
        result.push_back('=');
    }

    return result;
}

std::string Security::base64Decode(const std::string& input) {
    const std::string chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    std::string result;
    int val = 0, valb = -8;

    for (char c : input) {
        if (chars.find(c) == std::string::npos) break;
        val = (val << 6) + chars.find(c);
        valb += 6;
        if (valb >= 0) {
            result.push_back(char((val >> valb) & 0xFF));
            valb -= 8;
        }
    }

    return result;
}

std::vector<uint8_t> Security::generateRandomBytes(size_t length) {
    std::vector<uint8_t> bytes(length);

#ifdef _WIN32
    HCRYPTPROV hProv;
    if (CryptAcquireContext(&hProv, NULL, NULL, PROV_RSA_FULL, CRYPT_VERIFYCONTEXT)) {
        CryptGenRandom(hProv, length, bytes.data());
        CryptReleaseContext(hProv, 0);
    }
#elif defined(ESP_PLATFORM)
    // ESP32에서는 esp_random() 사용
    for (size_t i = 0; i < length; i++) {
        bytes[i] = esp_random() & 0xFF;
    }
#else
    if (getrandom(bytes.data(), length, 0) != (ssize_t)length) {
        // fallback to /dev/urandom
        std::ifstream urandom("/dev/urandom", std::ios::binary);
        urandom.read(reinterpret_cast<char*>(bytes.data()), length);
    }
#endif

    return bytes;
}

bool Security::constantTimeCompare(const std::string& a, const std::string& b) {
    if (a.length() != b.length()) {
        return false;
    }

    int result = 0;
    for (size_t i = 0; i < a.length(); i++) {
        result |= a[i] ^ b[i];
    }

    return result == 0;
}
