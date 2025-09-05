#include "core/config.h"
#include <fstream>
#include <sstream>
#include <algorithm>

Config::Config() {
}

Config::~Config() {
}

void Config::setString(const std::string& key, const std::string& value) {
    config_map_[key] = value;
}

std::string Config::getString(const std::string& key, const std::string& defaultValue) const {
    auto it = config_map_.find(key);
    return (it != config_map_.end()) ? it->second : defaultValue;
}

void Config::setInt(const std::string& key, int value) {
    config_map_[key] = std::to_string(value);
}

int Config::getInt(const std::string& key, int defaultValue) const {
    auto it = config_map_.find(key);
    if (it != config_map_.end()) {
        try {
            return std::stoi(it->second);
        } catch (const std::exception&) {
            return defaultValue;
        }
    }
    return defaultValue;
}

void Config::setFloat(const std::string& key, float value) {
    config_map_[key] = std::to_string(value);
}

float Config::getFloat(const std::string& key, float defaultValue) const {
    auto it = config_map_.find(key);
    if (it != config_map_.end()) {
        try {
            return std::stof(it->second);
        } catch (const std::exception&) {
            return defaultValue;
        }
    }
    return defaultValue;
}

void Config::setBool(const std::string& key, bool value) {
    config_map_[key] = value ? "true" : "false";
}

bool Config::getBool(const std::string& key, bool defaultValue) const {
    auto it = config_map_.find(key);
    if (it != config_map_.end()) {
        std::string value = it->second;
        std::transform(value.begin(), value.end(), value.begin(), ::tolower);
        return (value == "true" || value == "1" || value == "yes");
    }
    return defaultValue;
}

bool Config::saveToFile(const std::string& filename) const {
    try {
        std::ofstream file(filename);
        if (!file.is_open()) {
            return false;
        }
        
        for (const auto& pair : config_map_) {
            file << pair.first << "=" << serializeValue(pair.second) << std::endl;
        }
        
        return true;
    } catch (const std::exception&) {
        return false;
    }
}

bool Config::loadFromFile(const std::string& filename) {
    try {
        std::ifstream file(filename);
        if (!file.is_open()) {
            return false;
        }
        
        std::string line;
        while (std::getline(file, line)) {
            size_t pos = line.find('=');
            if (pos != std::string::npos) {
                std::string key = line.substr(0, pos);
                std::string value = line.substr(pos + 1);
                config_map_[key] = deserializeValue(value);
            }
        }
        
        return true;
    } catch (const std::exception&) {
        return false;
    }
}

bool Config::hasKey(const std::string& key) const {
    return config_map_.find(key) != config_map_.end();
}

std::vector<std::string> Config::getAllKeys() const {
    std::vector<std::string> keys;
    for (const auto& pair : config_map_) {
        keys.push_back(pair.first);
    }
    return keys;
}

void Config::clear() {
    config_map_.clear();
}

std::string Config::serializeValue(const std::string& value) const {
    std::string result = value;
    std::replace(result.begin(), result.end(), '\n', '\\');
    std::replace(result.begin(), result.end(), '\r', '\\');
    return result;
}

std::string Config::deserializeValue(const std::string& serialized) const {
    std::string result = serialized;
    std::replace(result.begin(), result.end(), '\\', '\n');
    return result;
}
