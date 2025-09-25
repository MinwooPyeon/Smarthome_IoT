#include "network/serial_controller.h"
#include "esp_log.h"
#include "driver/uart.h"
#include "driver/gpio.h"
#include "ArduinoJson.h"
#include "esp_wifi.h"
#include <algorithm>
#include <vector>
#include <cstring>

static const char* TAG = "SERIAL_CONTROLLER";

SerialController::SerialController(int baud_rate)
    : m_baud_rate(baud_rate), m_initialized(false), m_debug_mode(false),
      m_max_message_size(1024), m_max_messages_per_second(10), m_message_count(0) {
    m_input_buffer.reserve(MAX_BUFFER_SIZE);
    m_last_message_time = std::chrono::steady_clock::now();
}

SerialController::~SerialController() {
}

bool SerialController::initialize() {
    if (m_initialized) {
        return true;
    }

    uart_config_t uart_config = {
        .baud_rate = m_baud_rate,
        .data_bits = UART_DATA_8_BITS,
        .parity = UART_PARITY_DISABLE,
        .stop_bits = UART_STOP_BITS_1,
        .flow_ctrl = UART_HW_FLOWCTRL_DISABLE,
        .source_clk = UART_SCLK_REF_TICK,
    };

    ESP_ERROR_CHECK(uart_driver_install(UART_NUM_0, 1024 * 2, 0, 0, NULL, 0));
    ESP_ERROR_CHECK(uart_param_config(UART_NUM_0, &uart_config));
    ESP_ERROR_CHECK(uart_set_pin(UART_NUM_0, UART_PIN_NO_CHANGE, UART_PIN_NO_CHANGE, UART_PIN_NO_CHANGE, UART_PIN_NO_CHANGE));

    m_initialized = true;
    ESP_LOGI(TAG, "시리얼 통신 초기화 완료 (속도: %d bps)", m_baud_rate);

    DynamicJsonDocument init_doc(512);
    init_doc["type"] = "init";
    init_doc["status"] = "ready";
    init_doc["device_id"] = "esp-1";
    init_doc["version"] = "1.0.0";

    std::string init_str;
    serializeJson(init_doc, init_str);
    uart_write_bytes(UART_NUM_0, init_str.c_str(), init_str.length());
    uart_write_bytes(UART_NUM_0, "\n", 1);

    return true;
}

void SerialController::loop() {
    if (!m_initialized) {
        return;
    }

    uint8_t data[128];
    int len = uart_read_bytes(UART_NUM_0, data, sizeof(data) - 1, 0);

    if (len > 0) {
        data[len] = '\0';

        for (int i = 0; i < len; i++) {
            char c = data[i];

            if (c == '\n' || c == '\r') {
                continue;
            } else if (m_input_buffer.length() < MAX_BUFFER_SIZE - 1) {
                m_input_buffer += c;
            } else {
                ESP_LOGW(TAG, "입력 버퍼 오버플로우");
                m_input_buffer.clear();
            }
        }

        if (!m_input_buffer.empty()) {
            size_t newline_pos = m_input_buffer.find('\n');
            if (newline_pos == std::string::npos) {
                newline_pos = m_input_buffer.find('\r');
            }

            if (newline_pos != std::string::npos) {
                std::string command = m_input_buffer.substr(0, newline_pos);
                m_input_buffer = m_input_buffer.substr(newline_pos + 1);

                if (!command.empty()) {
                    processCommand(command);
                }
            } else {
                int open_braces = 0;
                int close_braces = 0;
                bool in_string = false;
                bool escaped = false;

                for (char c : m_input_buffer) {
                    if (escaped) {
                        escaped = false;
                        continue;
                    }
                    if (c == '\\') {
                        escaped = true;
                        continue;
                    }
                    if (c == '"') {
                        in_string = !in_string;
                        continue;
                    }
                    if (!in_string) {
                        if (c == '{') open_braces++;
                        else if (c == '}') close_braces++;
                    }
                }

                if ((open_braces > 0 && open_braces == close_braces && m_input_buffer.length() > 10) ||
                    isSimpleCommand(m_input_buffer)) {
                    processInput();
                    m_input_buffer.clear();
                }
            }
        }
    }
}

void SerialController::setCommandCallback(CommandCallback callback) {
    m_command_callback = callback;
}

void SerialController::sendResponse(const std::string& response) {
    if (m_initialized) {
        uart_write_bytes(UART_NUM_0, response.c_str(), response.length());
        uart_write_bytes(UART_NUM_0, "\n", 1);
        if (m_debug_mode) {
            ESP_LOGI(TAG, "응답 전송: %s", response.c_str());
        }
    }
}

void SerialController::sendError(const std::string& error_code, const std::string& error_message) {
    DynamicJsonDocument error_doc(512);
    error_doc["type"] = "error";
    error_doc["error_code"] = error_code.c_str();
    error_doc["message"] = error_message.c_str();
    error_doc["timestamp"] = esp_timer_get_time() / 1000;

    std::string error_str;
    serializeJson(error_doc, error_str);
    sendResponse(error_str.c_str());
}

void SerialController::sendStatus(const JsonObject& status) {
    DynamicJsonDocument status_doc(1024);
    status_doc["type"] = "status";
    status_doc["data"] = status;
    status_doc["timestamp"] = esp_timer_get_time() / 1000;

    std::string status_str;
    serializeJson(status_doc, status_str);
    sendResponse(status_str.c_str());
}

bool SerialController::isConnected() const {
    return m_initialized;
}

void SerialController::setDebugMode(bool enabled) {
    m_debug_mode = enabled;
    ESP_LOGI(TAG, "디버그 모드: %s", enabled ? "활성화" : "비활성화");
}

void SerialController::processInput() {
    if (m_input_buffer.empty()) {
        return;
    }

    if (m_debug_mode) {
        ESP_LOGI(TAG, "수신된 데이터: %s", m_input_buffer.c_str());
    }

    try {
        processCommand(m_input_buffer);
    } catch (const std::exception& e) {
        ESP_LOGE(TAG, "명령 처리 중 오류: %s", e.what());
        sendError("PROCESSING_ERROR", e.what());
    }
}

void SerialController::processCommand(const std::string& json_str) {
    if (!checkRateLimit()) {
        sendError("RATE_LIMIT_EXCEEDED", "속도 제한 초과");
        return;
    }

    std::string sanitized_input = sanitizeInput(json_str);

    if (m_debug_mode) {
        ESP_LOGI(TAG, "Sanitized input: %s", sanitized_input.c_str());
    }

    if (!validateJson(sanitized_input)) {
        if (isSimpleCommand(sanitized_input)) {
            processSimpleCommand(sanitized_input);
            return;
        } else {
            ESP_LOGE(TAG, "JSON 유효성 검증 실패: %s", sanitized_input.c_str());
            sendError("INVALID_JSON", "잘못된 JSON 형식");
            return;
        }
    }

    DynamicJsonDocument doc(1024);
    DeserializationError error = deserializeJson(doc, sanitized_input);

    if (error) {
        ESP_LOGE(TAG, "JSON 파싱 오류: %s", error.c_str());
        sendError("JSON_PARSE_ERROR", "JSON 파싱 실패");
        return;
    }

    if (!doc.containsKey("command") || !doc["command"].is<std::string>()) {
        sendError("MISSING_COMMAND", "명령어가 없습니다");
        return;
    }

    std::string command_str = doc["command"].as<std::string>();

    if (!validateCommand(command_str)) {
        sendError("INVALID_COMMAND", "허용되지 않은 명령어: " + command_str);
        return;
    }

    JsonObject params = doc.containsKey("params") ? doc["params"].as<JsonObject>() : JsonObject();

    std::string result;

    if (m_command_callback) {
        result = m_command_callback(command_str, params);
    } else {
        result = handleDefaultCommand(command_str, params);
    }

    DynamicJsonDocument response_doc(512);
    response_doc["type"] = "response";
    response_doc["command"] = command_str.c_str();
    response_doc["result"] = result.c_str();
    response_doc["timestamp"] = esp_timer_get_time() / 1000;

    std::string response_str;
    serializeJson(response_doc, response_str);
    sendResponse(response_str.c_str());
}

std::string SerialController::handleDefaultCommand(const std::string& command, const JsonObject& params) {
    if (command == "ping") {
        return "pong";
    } else if (command == "status") {
        DynamicJsonDocument status_doc(512);
        status_doc["device_id"] = "esp-1";
        status_doc["uptime"] = esp_timer_get_time() / 1000000;
        status_doc["free_heap"] = esp_get_free_heap_size();

        wifi_ap_record_t ap_info;
        bool wifi_connected = (esp_wifi_sta_get_ap_info(&ap_info) == ESP_OK);
        status_doc["wifi_connected"] = wifi_connected;

        std::string result;
        serializeJson(status_doc, result);
        return result;
    } else if (command == "help") {
        DynamicJsonDocument help_doc(512);
        JsonArray commands = help_doc.createNestedArray("available_commands");
        commands.add("ping");
        commands.add("status");
        commands.add("help");
        commands.add("ir_send");
        commands.add("raw_send");
        commands.add("ir_receive");
        commands.add("wifi_info");

        std::string result;
        serializeJson(help_doc, result);
        return result;
    } else if (command == "wifi_info") {
        DynamicJsonDocument wifi_doc(512);
        wifi_ap_record_t ap_info;
        if (esp_wifi_sta_get_ap_info(&ap_info) == ESP_OK) {
            wifi_doc["connected"] = true;
            wifi_doc["ssid"] = (char*)ap_info.ssid;
            wifi_doc["rssi"] = ap_info.rssi;
        } else {
            wifi_doc["connected"] = false;
        }

        std::string result;
        serializeJson(wifi_doc, result);
        return result;
    } else {
        return "알 수 없는 명령어: " + command;
    }
}

void SerialController::debugPrint(const std::string& message) {
    if (m_debug_mode) {
        ESP_LOGI(TAG, "%s", message.c_str());
    }
}

void SerialController::setAuthenticationToken(const std::string& token) {
    m_auth_token = token;
    ESP_LOGI(TAG, "인증 토큰 설정됨");
}

void SerialController::setMaxMessageSize(size_t max_size) {
    m_max_message_size = max_size;
    ESP_LOGI(TAG, "최대 메시지 크기 설정: %zu bytes", max_size);
}

void SerialController::setRateLimit(int max_messages_per_second) {
    m_max_messages_per_second = max_messages_per_second;
    ESP_LOGI(TAG, "속도 제한 설정: %d messages/sec", max_messages_per_second);
}

bool SerialController::validateCommand(const std::string& command) const {
    static const std::vector<std::string> allowed_commands = {
        "ping", "status", "ir_send", "raw_send", "ir_receive", "config_get", "config_set",
        "device_list", "device_control", "system_info", "restart",
        "mqtt_status", "ir_status",
        // Samsung TV 명령어
        "samsung_test", "samsung_power", "samsung_volume_up", "samsung_volume_down",
        // 에어컨 명령어
        "ac_power", "ac_temp_up", "ac_temp_down", "ac_fan_faster", "ac_fan_slower",
        "ac_cool", "ac_energy", "ac_fan_only", "ac_sleep", "ac_auto", "ac_timer", "ac_test",
        // IRremoteESP8266 라이브러리 테스트
        "irremote_test", "led_test", "simple_test", "ir_status", "hardware_test", "mqtt_debug", "mqtt_test_send", "device_info", "mqtt_resubscribe", "raw_limits", "ir_test"
    };

    for (const auto& allowed : allowed_commands) {
        if (command == allowed) {
            return true;
        }
    }

    ESP_LOGW(TAG, "허용되지 않은 명령어: %s", command.c_str());
    return false;
}

bool SerialController::validateJson(const std::string& json_str) const {
    if (json_str.length() > m_max_message_size) {
        ESP_LOGW(TAG, "JSON 메시지가 너무 큼: %zu bytes", json_str.length());
        return false;
    }

    DynamicJsonDocument doc(1024);
    DeserializationError error = deserializeJson(doc, json_str);
    if (error) {
        ESP_LOGW(TAG, "잘못된 JSON 형식: %s", error.c_str());
        return false;
    }

    return true;
}

std::string SerialController::sanitizeInput(const std::string& input) const {
    std::string sanitized = input;

    sanitized.erase(std::remove(sanitized.begin(), sanitized.end(), '\0'), sanitized.end());

    sanitized.erase(std::remove_if(sanitized.begin(), sanitized.end(),
        [](char c) {
            return c < 32 && c != '\t' && c != '\n' && c != '\r';
        }), sanitized.end());

    if (sanitized.length() > m_max_message_size) {
        sanitized = sanitized.substr(0, m_max_message_size);
    }

    return sanitized;
}

bool SerialController::checkRateLimit() {
    auto now = std::chrono::steady_clock::now();
    auto elapsed = std::chrono::duration_cast<std::chrono::seconds>(now - m_last_message_time);

    if (elapsed.count() >= 1) {
        m_message_count = 0;
        m_last_message_time = now;
    }

    if (m_message_count >= m_max_messages_per_second) {
        ESP_LOGW(TAG, "속도 제한 초과: %d messages/sec", m_max_messages_per_second);
        return false;
    }

    m_message_count++;
    return true;
}

bool SerialController::isSimpleCommand(const std::string& input) const {
    static const std::vector<std::string> simple_commands = {
        "ac_both"
    };

    for (const auto& cmd : simple_commands) {
        if (input == cmd) {
            return true;
        }
    }

    return false;
}

void SerialController::processSimpleCommand(const std::string& command) {
    ESP_LOGI(TAG, "단순 명령어 처리: %s", command.c_str());

    std::string result;

    if (m_command_callback) {
        JsonObject empty_params;
        result = m_command_callback(command, empty_params);
    } else {
        result = handleDefaultCommand(command, JsonObject());
    }

    sendResponse(result);
}
