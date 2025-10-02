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

enum class CryptoBackend {
    MBEDTLS,    // ESP32
    OPENSSL,    // Linux/macOS
    CRYPTOAPI   // Windows
};


class Security {
private:
    static CryptoBackend backend_;
    static bool initialized_;
    static bool initializeBackend();
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

    static bool initialize();
    static CryptoBackend getBackend() { return backend_; }
    static void cleanup();
public:

    static std::string sha256(const std::string& input);
    static std::string hmacSha256(const std::string& key, const std::string& message);
    static std::string generateSecureToken(size_t length = 32);
    static bool verifyToken(const std::string& token, const std::string& expected);
    static bool verifyTimeBasedToken(const std::string& token, const std::string& secret, int window = 30);
    static std::string encryptAES(const std::string& plaintext, const std::string& key);
    static std::string decryptAES(const std::string& ciphertext, const std::string& key);
    static std::string encryptAES_CBC(const std::string& plaintext, const std::string& key, const std::string& iv);
    static std::string decryptAES_CBC(const std::string& ciphertext, const std::string& key, const std::string& iv);
    static std::string hashPassword(const std::string& password, int rounds = 12);
    static bool verifyPassword(const std::string& password, const std::string& hash);
    static std::string sanitizeInput(const std::string& input);
    static std::string escapeSql(const std::string& input);
    static std::string escapeHtml(const std::string& input);
    static bool validateFilePath(const std::string& path);
    static bool validateIPAddress(const std::string& ip);
    static bool validateURL(const std::string& url);
    static bool validateEmail(const std::string& email);
    static bool validateStrongPassword(const std::string& password);
    static void logSecurityEvent(const std::string& level, const std::string& message, const std::string& details = "");
    static bool validateSecurityPolicy(const std::string& policy, const std::string& value);
private:
    static std::string base64Encode(const std::string& input);
    static std::string base64Decode(const std::string& input);
    static std::vector<uint8_t> generateRandomBytes(size_t length);
    static bool constantTimeCompare(const std::string& a, const std::string& b);
};
