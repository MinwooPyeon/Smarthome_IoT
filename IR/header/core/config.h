#pragma once

#include <string>
#include <memory>
#include <map>
#include <vector>

/**
 * @brief 애플리케이션 설정 클래스
 *
 * 웹 서버, MQTT, 보안 등의 설정을 관리합니다.
 */
class Config {
public:
    Config() = default;
    ~Config() = default;

    // 복사 생성자 및 할당 연산자
    Config(const Config&) = default;
    Config& operator=(const Config&) = default;

    // 이동 생성자 및 할당 연산자
    Config(Config&&) = default;
    Config& operator=(Config&&) = default;

    /**
     * @brief 파일에서 설정 로드
     * @param filename 설정 파일 경로
     * @return 설정 객체 (실패시 nullptr)
     */
    static std::shared_ptr<Config> loadFromFile(const std::string& filename);

    /**
     * @brief 기본 설정으로 로드
     * @return 기본 설정 객체
     */
    static std::shared_ptr<Config> loadDefault();

    /**
     * @brief 설정을 파일에 저장
     * @param filename 저장할 파일 경로
     * @return 저장 성공 여부
     */
    bool saveToFile(const std::string& filename) const;

    // 웹 서버 설정
    int getWebUIPort() const { return web_ui_port_; }
    void setWebUIPort(int port) { web_ui_port_ = port; }

    std::string getWebUIHost() const { return web_ui_host_; }
    void setWebUIHost(const std::string& host) { web_ui_host_ = host; }

    bool isWebUIEnabled() const { return web_ui_enabled_; }
    void setWebUIEnabled(bool enabled) { web_ui_enabled_ = enabled; }

    // MQTT 설정
    std::string getMqttBroker() const { return mqtt_broker_; }
    void setMqttBroker(const std::string& broker) { mqtt_broker_ = broker; }

    int getMqttPort() const { return mqtt_port_; }
    void setMqttPort(int port) { mqtt_port_ = port; }

    std::string getMqttUsername() const { return mqtt_username_; }
    void setMqttUsername(const std::string& username) { mqtt_username_ = username; }

    std::string getMqttPassword() const { return mqtt_password_; }
    void setMqttPassword(const std::string& password) { mqtt_password_ = password; }

    std::string getMqttClientId() const { return mqtt_client_id_; }
    void setMqttClientId(const std::string& client_id) { mqtt_client_id_ = client_id; }

    std::string getMqttTopicPrefix() const { return mqtt_topic_prefix_; }
    void setMqttTopicPrefix(const std::string& prefix) { mqtt_topic_prefix_ = prefix; }

    bool isMqttEnabled() const { return mqtt_enabled_; }
    void setMqttEnabled(bool enabled) { mqtt_enabled_ = enabled; }

    // 보안 설정
    std::string getApiToken() const { return api_token_; }
    void setApiToken(const std::string& token) { api_token_ = token; }

    bool isApiTokenRequired() const { return api_token_required_; }
    void setApiTokenRequired(bool required) { api_token_required_ = required; }

    std::vector<std::string> getAllowedOrigins() const { return allowed_origins_; }
    void setAllowedOrigins(const std::vector<std::string>& origins) { allowed_origins_ = origins; }

    // 추가 보안 설정
    bool isTlsEnabled() const { return tls_enabled_; }
    void setTlsEnabled(bool enabled) { tls_enabled_ = enabled; }

    std::string getTlsCertPath() const { return tls_cert_path_; }
    void setTlsCertPath(const std::string& path) { tls_cert_path_ = path; }

    std::string getTlsKeyPath() const { return tls_key_path_; }
    void setTlsKeyPath(const std::string& path) { tls_key_path_ = path; }

    std::string getTlsCaPath() const { return tls_ca_path_; }
    void setTlsCaPath(const std::string& path) { tls_ca_path_ = path; }

    bool isRateLimitEnabled() const { return rate_limit_enabled_; }
    void setRateLimitEnabled(bool enabled) { rate_limit_enabled_ = enabled; }

    int getRateLimitRequests() const { return rate_limit_requests_; }
    void setRateLimitRequests(int requests) { rate_limit_requests_ = requests; }

    int getRateLimitWindow() const { return rate_limit_window_; }
    void setRateLimitWindow(int window) { rate_limit_window_ = window; }

    bool isInputValidationEnabled() const { return input_validation_enabled_; }
    void setInputValidationEnabled(bool enabled) { input_validation_enabled_ = enabled; }

    bool isLoggingEnabled() const { return security_logging_enabled_; }
    void setLoggingEnabled(bool enabled) { security_logging_enabled_ = enabled; }

    // 로깅 설정
    std::string getLogLevel() const { return log_level_; }
    void setLogLevel(const std::string& level) { log_level_ = level; }

    std::string getLogFile() const { return log_file_; }
    void setLogFile(const std::string& file) { log_file_ = file; }

    bool isLogToFile() const { return log_to_file_; }
    void setLogToFile(bool enabled) { log_to_file_ = enabled; }

    // IR 설정
    std::string getIrDevice() const { return ir_device_; }
    void setIrDevice(const std::string& device) { ir_device_ = device; }

    int getIrTimeout() const { return ir_timeout_; }
    void setIrTimeout(int timeout) { ir_timeout_ = timeout; }

    int getIrRetryCount() const { return ir_retry_count_; }
    void setIrRetryCount(int count) { ir_retry_count_ = count; }

    // 사용자 정의 설정
    std::string getCustomValue(const std::string& key) const;
    void setCustomValue(const std::string& key, const std::string& value);
    bool hasCustomValue(const std::string& key) const;
    void removeCustomValue(const std::string& key);

    /**
     * @brief 설정 유효성 검사
     * @return 유효성 여부
     */
    bool isValid() const;

    /**
     * @brief 설정을 JSON 문자열로 변환
     * @return JSON 문자열
     */
    std::string toJson() const;

    /**
     * @brief JSON 문자열에서 설정 로드
     * @param json JSON 문자열
     * @return 로드 성공 여부
     */
    bool fromJson(const std::string& json);

    // 추가 메서드들 (기존 기능과 호환성 유지)
    bool load(const std::string& filename);
    int getInt(const std::string& key, int default_value) const;
    std::string getString(const std::string& key, const std::string& default_value) const;

    // 기존 인터페이스 호환성
    void setString(const std::string& key, const std::string& value);
    void setInt(const std::string& key, int value);
    void setFloat(const std::string& key, float value);
    void setBool(const std::string& key, bool value);
    float getFloat(const std::string& key, float defaultValue = 0.0f) const;
    bool getBool(const std::string& key, bool defaultValue = false) const;
    bool hasKey(const std::string& key) const;
    std::vector<std::string> getAllKeys() const;
    void clear();

private:
    // 웹 서버 설정
    int web_ui_port_ = 8080;
    std::string web_ui_host_ = "0.0.0.0";
    bool web_ui_enabled_ = true;

    // MQTT 설정
    std::string mqtt_broker_ = "";
    int mqtt_port_ = 1883;
    std::string mqtt_username_ = "";
    std::string mqtt_password_ = "";
    std::string mqtt_client_id_ = "irremote_client";
    std::string mqtt_topic_prefix_ = "irremote";
    bool mqtt_enabled_ = false;

    // 보안 설정
    std::string api_token_ = "";
    bool api_token_required_ = false;
    std::vector<std::string> allowed_origins_ = {"*"};

    // 추가 보안 설정
    bool tls_enabled_ = false;
    std::string tls_cert_path_ = "";
    std::string tls_key_path_ = "";
    std::string tls_ca_path_ = "";
    bool rate_limit_enabled_ = true;
    int rate_limit_requests_ = 100;
    int rate_limit_window_ = 60; // seconds
    bool input_validation_enabled_ = true;
    bool security_logging_enabled_ = true;

    // 로깅 설정
    std::string log_level_ = "INFO";
    std::string log_file_ = "/var/log/irremote.log";
    bool log_to_file_ = false;

    // IR 설정
    std::string ir_device_ = "/dev/lirc0";
    int ir_timeout_ = 5000;  // milliseconds
    int ir_retry_count_ = 3;

    // 사용자 정의 설정
    std::map<std::string, std::string> custom_values_;
};
