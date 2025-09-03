#include "core/config.h"
#include <fstream>
#include <iostream>
#include <filesystem>
#include <cstdlib>
#include "../external/nlohmann/json.hpp"

std::shared_ptr<Config> Config::loadFromFile(const std::string& path) {
    auto config = std::make_shared<Config>();
    
    std::ifstream file(path);
    if (!file.is_open()) {
        std::cerr << "Failed to open config file: " << path << std::endl;
        return config;
    }

    std::string content((std::istreambuf_iterator<char>(file)),
                        std::istreambuf_iterator<char>());
    file.close();

    try {
        // Parse JSON directly using nlohmann/json
        auto json_config = nlohmann::json::parse(content);
        
        // Copy values from JSON to our config
        if (json_config.contains("web_ui_port")) {
            config->setWebUIPort(json_config["web_ui_port"]);
        }
        if (json_config.contains("web_ui_host")) {
            config->setWebUIHost(json_config["web_ui_host"]);
        }
        if (json_config.contains("mqtt_broker")) {
            config->setMqttBroker(json_config["mqtt_broker"]);
        }
        if (json_config.contains("mqtt_port")) {
            config->setMqttPort(json_config["mqtt_port"]);
        }
        if (json_config.contains("mqtt_username")) {
            config->setMqttUsername(json_config["mqtt_username"]);
        }
        if (json_config.contains("mqtt_password")) {
            config->setMqttPassword(json_config["mqtt_password"]);
        }
        if (json_config.contains("mqtt_client_id")) {
            config->setMqttClientId(json_config["mqtt_client_id"]);
        }
        if (json_config.contains("api_token")) {
            config->setApiToken(json_config["api_token"]);
        }
        if (json_config.contains("log_level")) {
            config->setLogLevel(json_config["log_level"]);
        }
        if (json_config.contains("ir_device")) {
            config->setIrDevice(json_config["ir_device"]);
        }
        if (json_config.contains("ir_timeout")) {
            config->setIrTimeout(json_config["ir_timeout"]);
        }
        if (json_config.contains("ir_retry_count")) {
            config->setIrRetryCount(json_config["ir_retry_count"]);
        }
    } catch (const std::exception& e) {
        std::cerr << "Failed to parse config JSON: " << e.what() << std::endl;
        return config;
    }

    std::cout << "Loaded config from: " << path << std::endl;
    return config;
}

std::shared_ptr<Config> Config::loadDefault() {
    // Try to get home directory
    const char* home = std::getenv("HOME");
    if (!home) {
        std::cout << "Using default config (no HOME environment variable)" << std::endl;
        return std::make_shared<Config>();
    }

    std::filesystem::path configPath = std::string(home) + "/.config/irremote/irremote.conf.json";
    
    if (std::filesystem::exists(configPath)) {
        std::cout << "Using default config from: " << configPath << std::endl;
        return loadFromFile(configPath.string());
    }

    std::cout << "Using default config (no config file found)" << std::endl;
    return std::make_shared<Config>();
}

bool Config::saveToFile(const std::string& filename) const {
    try {
        std::ofstream file(filename);
        if (!file.is_open()) {
            return false;
        }
        
        file << toJson();
        file.close();
        return true;
    } catch (...) {
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
    return web_ui_port_ > 0 && web_ui_port_ < 65536;
}

std::string Config::toJson() const {
    nlohmann::json j;
    
    j["web_ui_port"] = web_ui_port_;
    j["web_ui_host"] = web_ui_host_;
    j["web_ui_enabled"] = web_ui_enabled_;
    j["mqtt_broker"] = mqtt_broker_;
    j["mqtt_port"] = mqtt_port_;
    j["mqtt_username"] = mqtt_username_;
    j["mqtt_password"] = mqtt_password_;
    j["mqtt_client_id"] = mqtt_client_id_;
    j["mqtt_topic_prefix"] = mqtt_topic_prefix_;
    j["mqtt_enabled"] = mqtt_enabled_;
    j["api_token"] = api_token_;
    j["api_token_required"] = api_token_required_;
    j["allowed_origins"] = allowed_origins_;
    j["log_level"] = log_level_;
    j["log_file"] = log_file_;
    j["log_to_file"] = log_to_file_;
    j["ir_device"] = ir_device_;
    j["ir_timeout"] = ir_timeout_;
    j["ir_retry_count"] = ir_retry_count_;
    j["custom_values"] = custom_values_;
    
    return j.dump(2);
}

bool Config::fromJson(const std::string& json) {
    try {
        auto j = nlohmann::json::parse(json);
        
        if (j.contains("web_ui_port")) web_ui_port_ = j["web_ui_port"];
        if (j.contains("web_ui_host")) web_ui_host_ = j["web_ui_host"];
        if (j.contains("web_ui_enabled")) web_ui_enabled_ = j["web_ui_enabled"];
        if (j.contains("mqtt_broker")) mqtt_broker_ = j["mqtt_broker"];
        if (j.contains("mqtt_port")) mqtt_port_ = j["mqtt_port"];
        if (j.contains("mqtt_username")) mqtt_username_ = j["mqtt_username"];
        if (j.contains("mqtt_password")) mqtt_password_ = j["mqtt_password"];
        if (j.contains("mqtt_client_id")) mqtt_client_id_ = j["mqtt_client_id"];
        if (j.contains("mqtt_topic_prefix")) mqtt_topic_prefix_ = j["mqtt_topic_prefix"];
        if (j.contains("mqtt_enabled")) mqtt_enabled_ = j["mqtt_enabled"];
        if (j.contains("api_token")) api_token_ = j["api_token"];
        if (j.contains("api_token_required")) api_token_required_ = j["api_token_required"];
        if (j.contains("allowed_origins")) allowed_origins_ = j["allowed_origins"].get<std::vector<std::string>>();
        if (j.contains("log_level")) log_level_ = j["log_level"];
        if (j.contains("log_file")) log_file_ = j["log_file"];
        if (j.contains("log_to_file")) log_to_file_ = j["log_to_file"];
        if (j.contains("ir_device")) ir_device_ = j["ir_device"];
        if (j.contains("ir_timeout")) ir_timeout_ = j["ir_timeout"];
        if (j.contains("ir_retry_count")) ir_retry_count_ = j["ir_retry_count"];
        if (j.contains("custom_values")) custom_values_ = j["custom_values"].get<std::map<std::string, std::string>>();
        
        return true;
    } catch (const std::exception& e) {
        return false;
    }
}
