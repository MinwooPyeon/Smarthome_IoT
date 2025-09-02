#include "config.h"
#include <fstream>
#include <iostream>
#include <filesystem>
#include <cstdlib>
#include <nlohmann/json.hpp>

namespace irremote {

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
        if (json_config.contains("token")) {
            config->setToken(json_config["token"]);
        }
        if (json_config.contains("port")) {
            config->setPort(json_config["port"]);
        }
        if (json_config.contains("webui_port")) {
            config->setWebUIPort(json_config["webui_port"]);
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

} // namespace irremote
