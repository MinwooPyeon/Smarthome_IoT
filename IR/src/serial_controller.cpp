#include "network/serial_controller.h"
#include "esp_log.h"
#include "driver/uart.h"
#include "driver/gpio.h"
#include "cJSON.h"

static const char* TAG = "SERIAL_CONTROLLER";

SerialController::SerialController(int baud_rate) 
    : m_baud_rate(baud_rate), m_initialized(false), m_debug_mode(false) {
    m_input_buffer.reserve(MAX_BUFFER_SIZE);
}

SerialController::~SerialController() {
    // 정리 작업
}

bool SerialController::initialize() {
    if (m_initialized) {
        return true;
    }

    // UART 설정
    uart_config_t uart_config = {
        .baud_rate = m_baud_rate,
        .data_bits = UART_DATA_8_BITS,
        .parity = UART_PARITY_DISABLE,
        .stop_bits = UART_STOP_BITS_1,
        .flow_ctrl = UART_HW_FLOWCTRL_DISABLE,
        .source_clk = UART_SCLK_DEFAULT,
    };
    
    // UART 드라이버 설치
    ESP_ERROR_CHECK(uart_driver_install(UART_NUM_0, 1024 * 2, 0, 0, NULL, 0));
    ESP_ERROR_CHECK(uart_param_config(UART_NUM_0, &uart_config));
    ESP_ERROR_CHECK(uart_set_pin(UART_NUM_0, UART_PIN_NO_CHANGE, UART_PIN_NO_CHANGE, UART_PIN_NO_CHANGE, UART_PIN_NO_CHANGE));

    m_initialized = true;
    ESP_LOGI(TAG, "시리얼 통신 초기화 완료 (속도: %d bps)", m_baud_rate);
    
    // 초기화 완료 메시지 전송
    cJSON *init_msg = cJSON_CreateObject();
    cJSON_AddStringToObject(init_msg, "type", "init");
    cJSON_AddStringToObject(init_msg, "status", "ready");
    cJSON_AddStringToObject(init_msg, "device_id", "esp32_ir_controller");
    cJSON_AddStringToObject(init_msg, "version", "1.0.0");
    
    char *init_str = cJSON_Print(init_msg);
    uart_write_bytes(UART_NUM_0, init_str, strlen(init_str));
    uart_write_bytes(UART_NUM_0, "\n", 1);
    free(init_str);
    cJSON_Delete(init_msg);
    
    return true;
}

void SerialController::loop() {
    if (!m_initialized) {
        return;
    }

    // UART 데이터 수신 처리
    uint8_t data[128];
    int len = uart_read_bytes(UART_NUM_0, data, sizeof(data) - 1, 0);
    
    if (len > 0) {
        data[len] = '\0';
        
        for (int i = 0; i < len; i++) {
            char c = data[i];
            
            if (c == '\n' || c == '\r') {
                if (!m_input_buffer.empty()) {
                    processInput();
                    m_input_buffer.clear();
                }
            } else if (m_input_buffer.length() < MAX_BUFFER_SIZE - 1) {
                m_input_buffer += c;
            } else {
                // 버퍼 오버플로우
                ESP_LOGW(TAG, "입력 버퍼 오버플로우");
                m_input_buffer.clear();
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
    cJSON *error_doc = cJSON_CreateObject();
    cJSON_AddStringToObject(error_doc, "type", "error");
    cJSON_AddStringToObject(error_doc, "error_code", error_code.c_str());
    cJSON_AddStringToObject(error_doc, "message", error_message.c_str());
    cJSON_AddNumberToObject(error_doc, "timestamp", esp_timer_get_time() / 1000);
    
    char *error_str = cJSON_Print(error_doc);
    sendResponse(error_str);
    free(error_str);
    cJSON_Delete(error_doc);
}

void SerialController::sendStatus(const cJSON* status) {
    cJSON *status_doc = cJSON_CreateObject();
    cJSON_AddStringToObject(status_doc, "type", "status");
    cJSON_AddItemToObject(status_doc, "data", cJSON_Duplicate(status, 1));
    cJSON_AddNumberToObject(status_doc, "timestamp", esp_timer_get_time() / 1000);
    
    char *status_str = cJSON_Print(status_doc);
    sendResponse(status_str);
    free(status_str);
    cJSON_Delete(status_doc);
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
    cJSON *doc = cJSON_Parse(json_str.c_str());
    
    if (doc == NULL) {
        ESP_LOGE(TAG, "JSON 파싱 오류");
        sendError("JSON_PARSE_ERROR", "JSON 파싱 실패");
        return;
    }

    cJSON *command = cJSON_GetObjectItem(doc, "command");
    if (!cJSON_IsString(command)) {
        sendError("MISSING_COMMAND", "명령어가 없습니다");
        cJSON_Delete(doc);
        return;
    }

    std::string command_str = command->valuestring;
    cJSON *params = cJSON_GetObjectItem(doc, "params");
    
    std::string result;
    
    if (m_command_callback) {
        result = m_command_callback(command_str, params);
    } else {
        result = handleDefaultCommand(command_str, params);
    }

    // 응답 전송
    cJSON *response_doc = cJSON_CreateObject();
    cJSON_AddStringToObject(response_doc, "type", "response");
    cJSON_AddStringToObject(response_doc, "command", command_str.c_str());
    cJSON_AddStringToObject(response_doc, "result", result.c_str());
    cJSON_AddNumberToObject(response_doc, "timestamp", esp_timer_get_time() / 1000);
    
    char *response_str = cJSON_Print(response_doc);
    sendResponse(response_str);
    free(response_str);
    cJSON_Delete(response_doc);
    cJSON_Delete(doc);
}

std::string SerialController::handleDefaultCommand(const std::string& command, const cJSON* params) {
    if (command == "ping") {
        return "pong";
    } else if (command == "status") {
        cJSON *status = cJSON_CreateObject();
        cJSON_AddStringToObject(status, "device_id", "esp32_ir_controller");
        cJSON_AddNumberToObject(status, "uptime", esp_timer_get_time() / 1000000); // 초 단위
        cJSON_AddNumberToObject(status, "free_heap", esp_get_free_heap_size());
        
        // WiFi 상태 확인
        wifi_ap_record_t ap_info;
        bool wifi_connected = (esp_wifi_sta_get_ap_info(&ap_info) == ESP_OK);
        cJSON_AddBoolToObject(status, "wifi_connected", wifi_connected);
        
        char *status_str = cJSON_Print(status);
        std::string result = status_str;
        free(status_str);
        cJSON_Delete(status);
        return result;
    } else if (command == "help") {
        cJSON *help = cJSON_CreateObject();
        cJSON *commands = cJSON_CreateArray();
        cJSON_AddItemToArray(commands, cJSON_CreateString("ping"));
        cJSON_AddItemToArray(commands, cJSON_CreateString("status"));
        cJSON_AddItemToArray(commands, cJSON_CreateString("help"));
        cJSON_AddItemToArray(commands, cJSON_CreateString("ir_send"));
        cJSON_AddItemToArray(commands, cJSON_CreateString("ir_receive"));
        cJSON_AddItemToArray(commands, cJSON_CreateString("wifi_info"));
        cJSON_AddItemToObject(help, "available_commands", commands);
        
        char *help_str = cJSON_Print(help);
        std::string result = help_str;
        free(help_str);
        cJSON_Delete(help);
        return result;
    } else if (command == "wifi_info") {
        cJSON *wifi_info = cJSON_CreateObject();
        wifi_ap_record_t ap_info;
        if (esp_wifi_sta_get_ap_info(&ap_info) == ESP_OK) {
            cJSON_AddBoolToObject(wifi_info, "connected", true);
            cJSON_AddStringToObject(wifi_info, "ssid", (char*)ap_info.ssid);
            cJSON_AddNumberToObject(wifi_info, "rssi", ap_info.rssi);
        } else {
            cJSON_AddBoolToObject(wifi_info, "connected", false);
        }
        
        char *wifi_str = cJSON_Print(wifi_info);
        std::string result = wifi_str;
        free(wifi_str);
        cJSON_Delete(wifi_info);
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
