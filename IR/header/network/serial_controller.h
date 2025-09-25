#pragma once

#include <string>
#include <functional>
#include <chrono>
#include "ArduinoJson.h"


class SerialController {
public:
    using CommandCallback = std::function<std::string(const std::string& command, const JsonObject& params)>;
    SerialController(int baud_rate = 115200);
    ~SerialController();
    bool initialize();
    void loop();
    void setCommandCallback(CommandCallback callback);
    void sendResponse(const std::string& response);
    void sendError(const std::string& error_code, const std::string& error_message);
    void sendStatus(const JsonObject& status);
    bool isConnected() const;
    void setDebugMode(bool enabled);

    void setAuthenticationToken(const std::string& token);
    void setMaxMessageSize(size_t max_size);
    void setRateLimit(int max_messages_per_second);
    bool validateCommand(const std::string& command) const;
    bool validateJson(const std::string& json_str) const;
    std::string sanitizeInput(const std::string& input) const;

private:
    int m_baud_rate;
    bool m_initialized;
    bool m_debug_mode;
    CommandCallback m_command_callback;

    std::string m_auth_token;
    size_t m_max_message_size;
    int m_max_messages_per_second;
    std::chrono::steady_clock::time_point m_last_message_time;
    int m_message_count;

    std::string m_input_buffer;
    static const size_t MAX_BUFFER_SIZE = 1024;
    void processInput();
    void processCommand(const std::string& json_str);
    std::string handleDefaultCommand(const std::string& command, const JsonObject& params);
    void debugPrint(const std::string& message);
    bool checkRateLimit();
    bool isSimpleCommand(const std::string& input) const;
    void processSimpleCommand(const std::string& command);
};
