#include "core/config.h"
#include <fstream>
#include <sstream>
#include <algorithm>
#include <nlohmann/json.hpp>

std::shared_ptr<Config> Config::loadFromFile(const std::string& filename) {
    auto config = std::make_shared<Config>();
    if (config->load(filename)) {
        return config;
    }
    return nullptr;
}

std::shared_ptr<Config> Config::loadDefault() {
    auto config = std::make_shared<Config>();
    // 기본 설정값들 설정
    config->setWebUIPort(8080);
    config->setWebUIHost("0.0.0.0");
    config->setWebUIEnabled(true);
    config->setMqttBroker("");
    config->setMqttPort(1883);
    config->setMqttClientId("irremote_client");
    config->setMqttTopicPrefix("irremote");
    config->setMqttEnabled(false);
    config->setLogLevel("INFO");
    config->setLogToFile(false);
    config->setIrDevice("/dev/lirc0");
    config->setIrTimeout(5000);
    config->setIrRetryCount(3);
    return config;
}

bool Config::saveToFile(const std::string& filename) const {
    try {
        std::ofstream file(filename);
        if (!file.is_open()) {
            return false;
        }
        
        file << toJson();
        return true;
    } catch (const std::exception& e) {
        return false;
    }
}

std::string Config::getCustomValue(const std::string& key) const {
    auto it = custom_values_.find(key);
    return (it != custom_values_.end()) ? it->second : "";
}

void Config::setCustomValue(const std::string& key, const std::string& value) {
    custom_values_[key] = value;
}

bool Config::hasCustomValue(const std::string& key) const {
    return custom_values_.find(key) != custom_values_.end();
}

void Config::removeCustomValue(const std::string& key) {
    custom_values_.erase(key);
}

bool Config::isValid() const {
    // 기본 유효성 검사
    if (web_ui_port_ <= 0 || web_ui_port_ > 65535) {
        return false;
    }
    if (mqtt_port_ <= 0 || mqtt_port_ > 65535) {
        return false;
    }
    if (ir_timeout_ < 0) {
        return false;
    }
    if (ir_retry_count_ < 0) {
        return false;
    }
    return true;
}

std::string Config::toJson() const {
    nlohmann::json config;
    
    // 웹 서버 설정
    config["web_ui"]["port"] = web_ui_port_;
    config["web_ui"]["host"] = web_ui_host_;
    config["web_ui"]["enabled"] = web_ui_enabled_;
    
    // MQTT 설정
    config["mqtt"]["broker"] = mqtt_broker_;
    config["mqtt"]["port"] = mqtt_port_;
    config["mqtt"]["username"] = mqtt_username_;
    config["mqtt"]["password"] = mqtt_password_;
    config["mqtt"]["client_id"] = mqtt_client_id_;
    config["mqtt"]["topic_prefix"] = mqtt_topic_prefix_;
    config["mqtt"]["enabled"] = mqtt_enabled_;
    
    // 보안 설정
    config["security"]["api_token"] = api_token_;
    config["security"]["api_token_required"] = api_token_required_;
    config["security"]["allowed_origins"] = allowed_origins_;
    
    // 로깅 설정
    config["logging"]["level"] = log_level_;
    config["logging"]["file"] = log_file_;
    config["logging"]["to_file"] = log_to_file_;
    
    // IR 설정
    config["ir"]["device"] = ir_device_;
    config["ir"]["timeout"] = ir_timeout_;
    config["ir"]["retry_count"] = ir_retry_count_;
    
    // 사용자 정의 설정
    config["custom"] = custom_values_;
    
    return config.dump(4);
}

bool Config::fromJson(const std::string& json) {
    try {
        nlohmann::json config = nlohmann::json::parse(json);
        
        // 웹 서버 설정
        if (config.contains("web_ui")) {
            auto web_ui = config["web_ui"];
            if (web_ui.contains("port")) web_ui_port_ = web_ui["port"];
            if (web_ui.contains("host")) web_ui_host_ = web_ui["host"];
            if (web_ui.contains("enabled")) web_ui_enabled_ = web_ui["enabled"];
        }
        
        // MQTT 설정
        if (config.contains("mqtt")) {
            auto mqtt = config["mqtt"];
            if (mqtt.contains("broker")) mqtt_broker_ = mqtt["broker"];
            if (mqtt.contains("port")) mqtt_port_ = mqtt["port"];
            if (mqtt.contains("username")) mqtt_username_ = mqtt["username"];
            if (mqtt.contains("password")) mqtt_password_ = mqtt["password"];
            if (mqtt.contains("client_id")) mqtt_client_id_ = mqtt["client_id"];
            if (mqtt.contains("topic_prefix")) mqtt_topic_prefix_ = mqtt["topic_prefix"];
            if (mqtt.contains("enabled")) mqtt_enabled_ = mqtt["enabled"];
        }
        
        // 보안 설정
        if (config.contains("security")) {
            auto security = config["security"];
            if (security.contains("api_token")) api_token_ = security["api_token"];
            if (security.contains("api_token_required")) api_token_required_ = security["api_token_required"];
            if (security.contains("allowed_origins")) allowed_origins_ = security["allowed_origins"];
        }
        
        // 로깅 설정
        if (config.contains("logging")) {
            auto logging = config["logging"];
            if (logging.contains("level")) log_level_ = logging["level"];
            if (logging.contains("file")) log_file_ = logging["file"];
            if (logging.contains("to_file")) log_to_file_ = logging["to_file"];
        }
        
        // IR 설정
        if (config.contains("ir")) {
            auto ir = config["ir"];
            if (ir.contains("device")) ir_device_ = ir["device"];
            if (ir.contains("timeout")) ir_timeout_ = ir["timeout"];
            if (ir.contains("retry_count")) ir_retry_count_ = ir["retry_count"];
        }
        
        // 사용자 정의 설정
        if (config.contains("custom")) {
            custom_values_ = config["custom"];
        }
        
        return true;
    } catch (const std::exception& e) {
        return false;
    }
}

bool Config::load(const std::string& filename) {
    std::ifstream file(filename);
    if (!file.is_open()) {
        return false;
    }
    
    std::string json((std::istreambuf_iterator<char>(file)),
                     std::istreambuf_iterator<char>());
    
    return fromJson(json);
}

int Config::getInt(const std::string& key, int default_value) const {
    auto it = custom_values_.find(key);
    if (it != custom_values_.end()) {
        try {
            return std::stoi(it->second);
        } catch (const std::exception& e) {
            return default_value;
        }
    }
    return default_value;
}

std::string Config::getString(const std::string& key, const std::string& default_value) const {
    auto it = custom_values_.find(key);
    return (it != custom_values_.end()) ? it->second : default_value;
}

// 기존 인터페이스 호환성 유지
void Config::setString(const std::string& key, const std::string& value) {
    setCustomValue(key, value);
}

void Config::setInt(const std::string& key, int value) {
    setCustomValue(key, std::to_string(value));
}

void Config::setFloat(const std::string& key, float value) {
    setCustomValue(key, std::to_string(value));
}

void Config::setBool(const std::string& key, bool value) {
    setCustomValue(key, value ? "true" : "false");
}

float Config::getFloat(const std::string& key, float defaultValue) const {
    auto it = custom_values_.find(key);
    if (it != custom_values_.end()) {
        try {
            return std::stof(it->second);
        } catch (const std::exception& e) {
            return defaultValue;
        }
    }
    return defaultValue;
}

bool Config::getBool(const std::string& key, bool defaultValue) const {
    auto it = custom_values_.find(key);
    if (it != custom_values_.end()) {
        std::string value = it->second;
        std::transform(value.begin(), value.end(), value.begin(), ::tolower);
        return (value == "true" || value == "1" || value == "yes");
    }
    return defaultValue;
}

bool Config::hasKey(const std::string& key) const {
    return hasCustomValue(key);
}

std::vector<std::string> Config::getAllKeys() const {
    std::vector<std::string> keys;
    for (const auto& pair : custom_values_) {
        keys.push_back(pair.first);
    }
    return keys;
}

void Config::clear() {
    custom_values_.clear();
}