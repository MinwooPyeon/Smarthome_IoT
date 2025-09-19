#include "core/config.h"
#include <fstream>
#include <sstream>
#include <algorithm>
#include <ArduinoJson.h>

std::shared_ptr<Config> Config::loadFromFile(const std::string& filename) {
    auto config = std::make_shared<Config>();
    if (config->load(filename)) {
        return config;
    }
    return nullptr;
}

std::shared_ptr<Config> Config::loadDefault() {
    auto config = std::make_shared<Config>();
    config->setWebUIPort(8080);
    config->setWebUIHost("0.0.0.0");
    config->setWebUIEnabled(true);
    config->setMqttBroker("");
    config->setMqttPort(8883);
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
    DynamicJsonDocument doc(2048);

    doc["web_ui"]["port"] = web_ui_port_;
    doc["web_ui"]["host"] = web_ui_host_.c_str();
    doc["web_ui"]["enabled"] = web_ui_enabled_;

    doc["mqtt"]["broker"] = mqtt_broker_.c_str();
    doc["mqtt"]["port"] = mqtt_port_;
    doc["mqtt"]["username"] = mqtt_username_.c_str();
    doc["mqtt"]["password"] = mqtt_password_.c_str();
    doc["mqtt"]["client_id"] = mqtt_client_id_.c_str();
    doc["mqtt"]["topic_prefix"] = mqtt_topic_prefix_.c_str();
    doc["mqtt"]["enabled"] = mqtt_enabled_;

    doc["security"]["api_token"] = api_token_.c_str();
    doc["security"]["api_token_required"] = api_token_required_;

    JsonArray origins = doc["security"].createNestedArray("allowed_origins");
    for (const auto& origin : allowed_origins_) {
        origins.add(origin.c_str());
    }

    doc["logging"]["level"] = log_level_.c_str();
    doc["logging"]["file"] = log_file_.c_str();
    doc["logging"]["to_file"] = log_to_file_;

    doc["ir"]["device"] = ir_device_.c_str();
    doc["ir"]["timeout"] = ir_timeout_;
    doc["ir"]["retry_count"] = ir_retry_count_;

    JsonObject custom = doc.createNestedObject("custom");
    for (const auto& pair : custom_values_) {
        custom[pair.first.c_str()] = pair.second.c_str();
    }

    std::string result;
    serializeJsonPretty(doc, result);
    return result;
}

bool Config::fromJson(const std::string& json) {
    DynamicJsonDocument doc(2048);
    DeserializationError error = deserializeJson(doc, json);

    if (error) {
        return false;
    }

    if (doc.containsKey("web_ui")) {
        JsonObject web_ui = doc["web_ui"];
        if (web_ui.containsKey("port")) web_ui_port_ = web_ui["port"];
        if (web_ui.containsKey("host")) web_ui_host_ = web_ui["host"].as<std::string>();
        if (web_ui.containsKey("enabled")) web_ui_enabled_ = web_ui["enabled"];
    }

    if (doc.containsKey("mqtt")) {
        JsonObject mqtt = doc["mqtt"];
        if (mqtt.containsKey("broker")) mqtt_broker_ = mqtt["broker"].as<std::string>();
        if (mqtt.containsKey("port")) mqtt_port_ = mqtt["port"];
        if (mqtt.containsKey("username")) mqtt_username_ = mqtt["username"].as<std::string>();
        if (mqtt.containsKey("password")) mqtt_password_ = mqtt["password"].as<std::string>();
        if (mqtt.containsKey("client_id")) mqtt_client_id_ = mqtt["client_id"].as<std::string>();
        if (mqtt.containsKey("topic_prefix")) mqtt_topic_prefix_ = mqtt["topic_prefix"].as<std::string>();
        if (mqtt.containsKey("enabled")) mqtt_enabled_ = mqtt["enabled"];
    }

    if (doc.containsKey("security")) {
        JsonObject security = doc["security"];
        if (security.containsKey("api_token")) api_token_ = security["api_token"].as<std::string>();
        if (security.containsKey("api_token_required")) api_token_required_ = security["api_token_required"];
        if (security.containsKey("allowed_origins")) {
            allowed_origins_.clear();
            JsonArray origins = security["allowed_origins"];
            for (JsonVariant origin : origins) {
                allowed_origins_.push_back(origin.as<std::string>());
            }
        }
    }

    if (doc.containsKey("logging")) {
        JsonObject logging = doc["logging"];
        if (logging.containsKey("level")) log_level_ = logging["level"].as<std::string>();
        if (logging.containsKey("file")) log_file_ = logging["file"].as<std::string>();
        if (logging.containsKey("to_file")) log_to_file_ = logging["to_file"];
    }

    if (doc.containsKey("ir")) {
        JsonObject ir = doc["ir"];
        if (ir.containsKey("device")) ir_device_ = ir["device"].as<std::string>();
        if (ir.containsKey("timeout")) ir_timeout_ = ir["timeout"];
        if (ir.containsKey("retry_count")) ir_retry_count_ = ir["retry_count"];
    }

    if (doc.containsKey("custom")) {
        JsonObject custom = doc["custom"];
        for (JsonPair pair : custom) {
            custom_values_[pair.key().c_str()] = pair.value().as<std::string>();
        }
    }

    return true;
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
