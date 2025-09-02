#pragma once

#include <string>
#include <memory>
#include <nlohmann/json.hpp>

namespace irremote {

class Config {
public:
    Config() = default;
    ~Config() = default;

    // Load configuration from file
    static std::shared_ptr<Config> loadFromFile(const std::string& path);
    static std::shared_ptr<Config> loadDefault();

    // Getters
    const std::string& getToken() const { return token_; }
    int getPort() const { return port_; }
    int getWebUIPort() const { return webui_port_; }
    
    // Setters
    void setToken(const std::string& token) { token_ = token; }
    void setPort(int port) { port_ = port; }
    void setWebUIPort(int port) { webui_port_ = port; }

private:
    std::string token_;
    int port_ = 9090;  // Default API port
    int webui_port_ = 8080;  // Default Web UI port
};

} // namespace irremote
