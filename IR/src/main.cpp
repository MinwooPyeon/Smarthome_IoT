#include <Arduino.h>
#include <stdio.h>
#include <string.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/queue.h"
#include "esp_system.h"
#include "esp_log.h"
#include "esp_err.h"
#include "nvs_flash.h"
#include "esp_wifi.h"
#include "esp_event.h"
#include "esp_netif.h"
#include "esp_timer.h"
#include "driver/gpio.h"
#include "driver/uart.h"
#include "ArduinoJson.h"
#include <vector>
#include <IRremoteESP8266.h>
#include <IRsend.h>
#include "WiFiClientSecure.h"
#include "PubSubClient.h"

#include "core/config.h"
#include "network/mqtt_client.h"
#include "network/serial_controller.h"
#include "hardware/irsend.h"

static const char* TAG = "IR_REMOTE_MAIN";

Config* g_config = nullptr;
MqttClient* g_mqtt_client = nullptr;
SerialController* g_serial_controller = nullptr;
IRSend* g_ir_sender = nullptr;

// IRremoteESP8266 라이브러리 사용
IRsend* g_irremote_sender = nullptr;

// Arduino 코드에서 검증된 에어컨 Raw IR 데이터 (38kHz)
// Arduino: {181,89, 12,11, ...} -> IRremoteESP8266: {1810, 890, 120, 110, ...} (10배 변환)
const uint16_t power_cmd[] = {1810,890, 120,110, 120,100, 120,100, 120,330, 120,110, 120,100, 120,110, 110,110, 120,330, 120,100, 120,330, 120,100, 120,330, 120,330, 120,330, 120,330, 120,330, 120,100, 120,100, 120,110, 120,330, 120,100, 120,110, 110,110, 120,100, 120,330, 120,330, 120,330, 120,100, 120,330, 120,330, 120,330, 120};
const uint16_t temp_up_cmd[] = {1810,890, 120,110, 120,100, 120,110, 120,320, 120,110, 120,100, 120,110, 120,100, 120,330, 120,100, 120,330, 120,110, 120,320, 120,340, 110,330, 120,330, 120,100, 120,330, 120,330, 120,330, 120,100, 120,110, 120,100, 120,100, 120,330, 120,110, 120,100, 120,110, 110,330, 120,330, 120,330, 120,330, 120};
const uint16_t temp_down_cmd[] = {1800,900, 120,100, 120,100, 120,110, 120,330, 120,100, 120,110, 110,110, 120,100, 120,330, 120,110, 120,320, 120,110, 120,330, 120,320, 120,330, 120,330, 120,330, 120,100, 120,330, 120,330, 120,100, 120,110, 120,100, 120,110, 120,100, 120,330, 120,100, 120,110, 120,330, 110,340, 110,330, 120,330, 120};
const uint16_t fan_slower_cmd[] = {1810,890, 120,110, 120,100, 120,110, 110,340, 110,110, 120,100, 120,110, 110,110, 120,330, 120,100, 120,330, 120,110, 110,340, 110,340, 110,330, 120,330, 120,100, 120,110, 120,320, 120,110, 120,100, 120,110, 120,100, 120,110, 110,330, 120,330, 120,110, 120,320, 120,330, 120,330, 120,330, 120,330, 120};
const uint16_t fan_faster_cmd[] = {1800,890, 120,110, 120,100, 120,110, 120,330, 110,110, 120,100, 120,110, 120,100, 120,330, 120,100, 120,330, 120,110, 120,330, 110,330, 120,330, 120,330, 120,330, 120,100, 120,110, 120,100, 120,100, 120,110, 120,100, 120,110, 120,100, 120,330, 120,330, 120,330, 110,330, 120,330, 120,330, 120,330, 120};
const uint16_t cool_cmd[] = {1800,900, 110,110, 120,100, 120,110, 120,330, 110,110, 120,100, 120,110, 120,100, 120,340, 110,110, 110,340, 110,110, 120,330, 110,340, 110,340, 110,330, 120,330, 120,100, 120,110, 120,330, 110,110, 120,100, 120,110, 120,100, 120,110, 110,340, 110,340, 110,110, 120,330, 110,340, 110,340, 110,330, 120};
const uint16_t energy_cmd[] = {1810,890, 110,120, 110,110, 120,100, 120,340, 110,110, 110,110, 120,100, 120,110, 120,330, 120,100, 120,330, 120,100, 120,340, 110,330, 120,330, 120,330, 120,100, 120,330, 120,100, 120,110, 120,100, 120,110, 110,110, 120,100, 120,330, 120,110, 110,340, 110,340, 110,340, 110,330, 120,330, 120,330, 110};
const uint16_t fan_only_cmd[] = {1810,890, 120,110, 120,100, 120,110, 120,330, 110,110, 120,100, 120,110, 120,100, 120,330, 120,100, 120,340, 110,110, 120,330, 110,330, 120,340, 110,330, 120,330, 120,330, 110,340, 110,110, 120,100, 120,110, 120,100, 120,110, 110,110, 120,100, 120,110, 120,330, 110,340, 110,330, 120,340, 110,330, 120};
const uint16_t sleep_cmd[] = {1800,900, 110,110, 120,100, 120,110, 120,330, 110,110, 120,100, 120,110, 120,100, 120,340, 110,110, 110,330, 120,110, 120,330, 110,340, 110,340, 110,340, 110,100, 120,110, 120,100, 120,110, 110,110, 120,110, 110,110, 120,100, 120,340, 110,330, 120,330, 120,330, 120,330, 110,340, 110,340, 110,330, 120};
const uint16_t auto_cmd[] = {1800,900, 110,110, 120,100, 120,110, 120,330, 120,100, 120,100, 120,110, 120,100, 120,330, 120,100, 120,330, 120,110, 120,330, 110,330, 120,330, 120,330, 120,330, 120,330, 120,330, 110,330, 120,110, 120,100, 120,110, 120,100, 120,100, 120,110, 120,100, 120,110, 120,320, 120,330, 120,330, 120,330, 120};
const uint16_t timer_cmd[] = {1810,890, 120,110, 120,100, 120,100, 120,330, 120,110, 120,100, 120,100, 120,110, 120,330, 120,100, 120,330, 120,100, 120,340, 110,330, 120,330, 120,330, 120,100, 120,330, 120,330, 120,100, 120,110, 110,110, 120,100, 120,110, 120,330, 120,100, 120,100, 120,340, 110,330, 120,330, 120,330, 120,330, 120};

// Arduino 코드에서 검증된 Raw 데이터를 IRremoteESP8266로 전송하는 함수
void sendArduinoRawData(const uint16_t* data, size_t length) {
    if (!data || length == 0) {
        ESP_LOGE(TAG, "Raw IR 데이터가 비어있음");
        return;
    }

    if (!g_irremote_sender) {
        ESP_LOGE(TAG, "IRremoteESP8266 송신기가 초기화되지 않음");
        return;
    }

    // Arduino 코드에서 검증된 Raw 데이터를 직접 전송 (이미 마이크로초 단위)
    g_irremote_sender->sendRaw(data, length, 38); // 38kHz 주파수
    ESP_LOGI(TAG, "Arduino 검증 Raw IR 데이터 전송 성공: %d개 펄스", (int)length);
}

WiFiClientSecure g_secure_client;
PubSubClient g_pubsub_client(g_secure_client);

#ifndef WIFI_SSID
#define WIFI_SSID "iPhone"
#endif
#ifndef WIFI_PASSWORD
#define WIFI_PASSWORD "49555412"
#endif
#ifndef MQTT_BROKER
#define MQTT_BROKER "43.201.62.254"
#endif
#ifndef MQTT_PORT
#define MQTT_PORT 8883
#endif
#ifndef MQTT_CLIENT_ID
#define MQTT_CLIENT_ID "controller-server"
#endif
#ifndef DEVICE_ID
#define DEVICE_ID "esp-1"
#endif
#ifndef MQTT_USERNAME
#define MQTT_USERNAME ""
#endif
#ifndef MQTT_PASSWORD
#define MQTT_PASSWORD ""
#endif
#ifndef MQTT_USE_TLS
#define MQTT_USE_TLS 0
#endif

TaskHandle_t mqtt_task_handle = NULL;

static void wifi_event_handler(void* arg, esp_event_base_t event_base,
                              int32_t event_id, void* event_data) {
    if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_START) {
        ESP_LOGI(TAG, "WiFi Station 시작");
    } else if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_DISCONNECTED) {
        ESP_LOGI(TAG, "WiFi 재연결 시도");
        esp_wifi_connect();
    } else if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_CONNECTED) {
        ESP_LOGI(TAG, "WiFi AP에 연결됨, IP 할당 대기");
    } else if (event_base == IP_EVENT && event_id == IP_EVENT_STA_GOT_IP) {
        ip_event_got_ip_t* event = (ip_event_got_ip_t*) event_data;
        ESP_LOGI(TAG, "WiFi 연결 성공");
        ESP_LOGI(TAG, "IP 주소: " IPSTR, IP2STR(&event->ip_info.ip));
        ESP_LOGI(TAG, "게이트웨이: " IPSTR, IP2STR(&event->ip_info.gw));
        ESP_LOGI(TAG, "넷마스크: " IPSTR, IP2STR(&event->ip_info.netmask));
    }
}

void initWiFi() {
    ESP_LOGI(TAG, "WiFi 초기화 시작");

    WiFi.mode(WIFI_STA);
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

    ESP_LOGI(TAG, "WiFi 연결 시도 - SSID: %s", WIFI_SSID);

    int wifi_wait_count = 0;
    const int max_wifi_wait = 20;

    while (WiFi.status() != WL_CONNECTED && wifi_wait_count < max_wifi_wait) {
        vTaskDelay(pdMS_TO_TICKS(1000));
        wifi_wait_count++;
        ESP_LOGI(TAG, "WiFi 연결 대기... (%d/%d)", wifi_wait_count, max_wifi_wait);
    }

    if (WiFi.status() == WL_CONNECTED) {
        ESP_LOGI(TAG, "WiFi 연결 성공!");
        ESP_LOGI(TAG, "SSID: %s", WiFi.SSID().c_str());
        ESP_LOGI(TAG, "IP: %s", WiFi.localIP().toString().c_str());
        ESP_LOGI(TAG, "RSSI: %d dBm", WiFi.RSSI());
    } else {
        ESP_LOGE(TAG, "WiFi 연결 실패. 상태: %d", WiFi.status());
    }
}


void sendErrorMessage(int tx_id, const std::string& error_type, const std::string& error_message) {
    DynamicJsonDocument error_doc(512);
    error_doc["tx_id"] = tx_id;
    error_doc["error"] = error_type.c_str();
    error_doc["message"] = error_message.c_str();

    std::string error_str;
    serializeJson(error_doc, error_str);
    std::string error_topic = "hub/" + std::string(DEVICE_ID) + "/error";

    if (g_pubsub_client.connected()) {
        g_pubsub_client.publish(error_topic.c_str(), error_str.c_str());
        ESP_LOGI(TAG, "오류 메시지 전송: %s", error_str.c_str());
    }
}


void sendErrorMessage(const std::string& level, const std::string& code,
                     const std::string& detail, const std::string& orderMsgId = "") {
    DynamicJsonDocument error_doc(512);
    error_doc["ts"] = esp_timer_get_time() / 1000;
    error_doc["deviceId"] = DEVICE_ID;
    error_doc["schema"] = "error/1.0";
    error_doc["level"] = level.c_str();
    error_doc["code"] = code.c_str();
    error_doc["detail"] = detail.c_str();

    if (!orderMsgId.empty()) {
        JsonObject ctx = error_doc.createNestedObject("ctx");
        ctx["orderMsgId"] = orderMsgId.c_str();
    }

    std::string error_str;
    serializeJson(error_doc, error_str);
    std::string error_topic = "hub/" + std::string(DEVICE_ID) + "/error";

    if (g_pubsub_client.connected()) {
        g_pubsub_client.publish(error_topic.c_str(), error_str.c_str());
        ESP_LOGI(TAG, "에러 메시지 전송: %s", error_str.c_str());
    }
}

void onMQTTMessage(char* topic, unsigned char* payload, unsigned int length) {
    std::string message((char*)payload, length);
    std::string topic_str = std::string(topic);
    ESP_LOGI(TAG, "=== MQTT 메시지 수신 ===");
    ESP_LOGI(TAG, "토픽: %s", topic);
    ESP_LOGI(TAG, "길이: %d bytes", length);
    ESP_LOGI(TAG, "내용: %s", message.c_str());
    ESP_LOGI(TAG, "========================");


    if (topic_str.find("/order/control") != std::string::npos) {
        ESP_LOGI(TAG, "IR 제어 명령 메시지 수신");

        DynamicJsonDocument doc(8192);
        DeserializationError error = deserializeJson(doc, message);
        if (error) {
            ESP_LOGE(TAG, "JSON 파싱 실패: %s", error.c_str());
            sendErrorMessage(-1, "INVALID_COMMAND", "JSON 파싱 실패 - 잘못된 JSON 형식입니다");
            return;
        }

        if (!doc.containsKey("tx_id") || !doc.containsKey("device_type") || !doc.containsKey("raw_data") ||
            !doc["tx_id"].is<int>() || !doc["device_type"].is<std::string>() || !doc["raw_data"].is<JsonArray>()) {
            ESP_LOGE(TAG, "필수 필드 누락 (tx_id, device_type, raw_data)");
            int error_tx_id = doc.containsKey("tx_id") && doc["tx_id"].is<int>() ? doc["tx_id"].as<int>() : -1;
            sendErrorMessage(error_tx_id, "INVALID_COMMAND", "필수 필드 누락 - tx_id, device_type, raw_data 중 일부가 없습니다");
            return;
        }

        int transaction_id = doc["tx_id"].as<int>();
        std::string device_type_str = doc["device_type"].as<std::string>();
        std::string function_str = doc.containsKey("function") && doc["function"].is<std::string>() ? doc["function"].as<std::string>() : "";

        ESP_LOGI(TAG, "트랜잭션 ID: %d", transaction_id);
        ESP_LOGI(TAG, "가전 타입: %s", device_type_str.c_str());
        ESP_LOGI(TAG, "기능: %s", function_str.c_str());

        if (doc.containsKey("meta_data") && doc["meta_data"].is<JsonArray>()) {
            JsonArray meta_data = doc["meta_data"];
            ESP_LOGI(TAG, "메타데이터 (%d개):", meta_data.size());
            int i = 0;
            for (JsonVariant item : meta_data) {
                if (item.is<std::string>()) {
                    ESP_LOGI(TAG, "  [%d]: %s", i, item.as<std::string>().c_str());
                }
                i++;
            }
        }

        JsonArray raw_data = doc["raw_data"];
        std::vector<int> raw_data_array;

        ESP_LOGI(TAG, "Raw 데이터 (%d개):", raw_data.size());
        int i = 0;
        for (JsonVariant item : raw_data) {
            if (item.is<int>()) {
                raw_data_array.push_back(item.as<int>());
                if (i < 10) {
                    ESP_LOGI(TAG, "  [%d]: %d", i, item.as<int>());
                }
            }
            i++;
        }

        if (raw_data.size() > 10) {
            ESP_LOGI(TAG, "총 %d개 데이터", raw_data.size());
        }

        if (g_ir_sender && !raw_data_array.empty()) {
            auto start_time = esp_timer_get_time();

            ESP_LOGI(TAG, "IR 신호 송신 시작 (Raw 데이터: %d개 펄스)", (int)raw_data_array.size());
            auto result = g_ir_sender->sendRawData(raw_data_array);

            auto duration = (esp_timer_get_time() - start_time) / 1000;
            ESP_LOGI(TAG, "IR 송신 완료 (소요시간: %lldms, 결과: %s)", duration,
                     result.result == IRSendResult::SUCCESS ? "성공" : "실패");

            if (result.result == IRSendResult::SUCCESS) {
                if (g_pubsub_client.connected()) {
                    DynamicJsonDocument response_doc(512);
                    response_doc["tx_id"] = transaction_id;
                    response_doc["status"] = "success";
                    response_doc["device_type"] = device_type_str.c_str();
                    response_doc["function"] = function_str.c_str();
                    response_doc["duration_ms"] = duration;
                    response_doc["timestamp"] = esp_timer_get_time() / 1000;

                    std::string response_str;
                    serializeJson(response_doc, response_str);
                    std::string response_topic = "hub/" + std::string(DEVICE_ID) + "/order/response";
                    g_pubsub_client.publish(response_topic.c_str(), response_str.c_str());
                    ESP_LOGI(TAG, "성공 응답 메시지 전송: %s", response_str.c_str());
                }
            } else {
                sendErrorMessage(transaction_id, "HARDWARE_ERROR", "IR 신호 전송 실패: " + result.message);
            }
        } else {
            ESP_LOGE(TAG, "IR 송신기 초기화되지 않았거나 Raw 데이터가 비어있음");
            sendErrorMessage(transaction_id, "HARDWARE_ERROR", "IR 송신기 초기화되지 않았거나 Raw 데이터가 비어있음");
        }

        return;
    }

    ESP_LOGW(TAG, "알 수 없는 토픽: %s", topic_str.c_str());
}

std::string onSerialCommand(const std::string& command, const JsonObject& params) {
    ESP_LOGI(TAG, "시리얼 명령 수신: %s", command.c_str());

    if (command == "raw_send") {
        if (!params.containsKey("raw_data") || !params["raw_data"].is<JsonArray>()) {
            return "오류: raw_data 배열이 필요";
        }

        JsonArray raw_data = params["raw_data"];
        std::vector<int> raw_data_array;

        for (JsonVariant item : raw_data) {
            if (item.is<int>()) {
                raw_data_array.push_back(item.as<int>());
            }
        }

        if (g_ir_sender && !raw_data_array.empty()) {
            auto result = g_ir_sender->sendRawData(raw_data_array);

            DynamicJsonDocument response_doc(512);
            response_doc["success"] = (result.result == IRSendResult::SUCCESS);
            response_doc["raw_data_count"] = raw_data.size();
            response_doc["message"] = result.message.c_str();

            std::string result_str;
            serializeJson(response_doc, result_str);
            return result_str;
        } else {
            return "오류: IR 송신기가 초기화되지 않았거나 Raw 데이터가 비어있음";
        }
    } else if (command == "ir_status") {
        DynamicJsonDocument ir_doc(256);
        ir_doc["sending"] = (g_ir_sender != nullptr);
        ir_doc["tx_pin"] = 23;

        std::string result_str;
        serializeJson(ir_doc, result_str);
        return result_str;
    } else if (command == "mqtt_status") {
        DynamicJsonDocument mqtt_doc(256);
        mqtt_doc["connected"] = g_pubsub_client.connected();
        mqtt_doc["broker"] = g_config ? g_config->getString("mqtt.broker", "").c_str() : "";
        mqtt_doc["port"] = g_config ? g_config->getInt("mqtt.port", 8883) : 8883;

        std::string result_str;
        serializeJson(mqtt_doc, result_str);
        return result_str;
    } else if (command == "ir_test") {
        if (g_ir_sender) {
            // 간단한 테스트 IR 신호 (Samsung TV 전원)
            std::vector<int> test_data = {9000, 4500, 560, 560, 560, 560, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 560, 560, 1690, 560, 560, 560, 1690, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560};

            ESP_LOGI(TAG, "IR Transmitter 테스트 시작 (GPIO 25)");
            auto result = g_ir_sender->sendRawData(test_data);

            DynamicJsonDocument response_doc(512);
            response_doc["success"] = (result.result == IRSendResult::SUCCESS);
            response_doc["message"] = result.message.c_str();
            response_doc["gpio_pin"] = 25;
            response_doc["test_data_count"] = test_data.size();

            std::string result_str;
            serializeJson(response_doc, result_str);
            return result_str;
        } else {
            return "오류: IR 송신기가 초기화되지 않음";
        }
    } else if (command == "raw_limits") {
        DynamicJsonDocument limits_doc(512);
        limits_doc["rmt_memory_blocks"] = 4;
        limits_doc["max_pulses"] = 512;  // 4 blocks * 64 items * 2 pulses
        limits_doc["json_buffer_size"] = 8192;
        limits_doc["mqtt_buffer_size"] = 1024;
        limits_doc["estimated_max_raw_data"] = 400;  // JSON 오버헤드 고려
        limits_doc["mqtt_max_message_size"] = 1024;  // MQTT 버퍼 크기

        std::string result_str;
        serializeJson(limits_doc, result_str);
        return result_str;
    } else if (command == "device_list") {
        DynamicJsonDocument device_doc(512);
        JsonArray devices = device_doc.createNestedArray("devices");

        // Samsung TV
        JsonObject tv = devices.createNestedObject();
        tv["id"] = "samsung_tv";
        tv["name"] = "Samsung TV";
        tv["type"] = "tv";

        // Samsung AC
        JsonObject ac = devices.createNestedObject();
        ac["id"] = "samsung_ac";
        ac["name"] = "Samsung AC";
        ac["type"] = "air_conditioner";

        std::string result_str;
        serializeJson(device_doc, result_str);
        return result_str;
    } else if (command == "samsung_test") {
        if (g_ir_sender) {
            DynamicJsonDocument response_doc(512);
            JsonArray results = response_doc.createNestedArray("results");

            // Samsung TV 테스트 명령어들 (RMT 방식)
            std::vector<std::pair<std::string, std::string>> test_commands = {
                {"power", "0x20DF10EF"},
                {"volume_up", "0x20DF40BF"},
                {"volume_down", "0x20DFC03F"},
                {"channel_up", "0x20DF00FF"},
                {"channel_down", "0x20DF807F"}
            };

            for (const auto& cmd : test_commands) {
                JsonObject result = results.createNestedObject();
                result["command"] = cmd.first.c_str();

                auto ir_result = g_ir_sender->sendIRCode(cmd.second);
                result["success"] = (ir_result.result == IRSendResult::SUCCESS);
                result["message"] = ir_result.message.c_str();

                vTaskDelay(pdMS_TO_TICKS(500)); // 0.5초 대기
            }

            response_doc["total_tests"] = test_commands.size();
            response_doc["library"] = "RMT";

            std::string result_str;
            serializeJson(response_doc, result_str);
            return result_str;
        } else {
            return "오류: IR 송신기가 초기화되지 않음";
        }
    } else if (command == "ac_power") {
        ESP_LOGI(TAG, "에어컨 전원 명령 실행 (Arduino 검증 데이터)");
        sendArduinoRawData(power_cmd, sizeof(power_cmd)/sizeof(power_cmd[0]));
        return "에어컨 전원 명령 전송 완료";
    } else if (command == "ac_temp_up") {
        ESP_LOGI(TAG, "에어컨 온도 상승 명령 실행 (Arduino 검증 데이터)");
        sendArduinoRawData(temp_up_cmd, sizeof(temp_up_cmd)/sizeof(temp_up_cmd[0]));
        return "에어컨 온도 상승 명령 전송 완료";
    } else if (command == "ac_temp_down") {
        ESP_LOGI(TAG, "에어컨 온도 하강 명령 실행 (Arduino 검증 데이터)");
        sendArduinoRawData(temp_down_cmd, sizeof(temp_down_cmd)/sizeof(temp_down_cmd[0]));
        return "에어컨 온도 하강 명령 전송 완료";
    } else if (command == "ac_fan_faster") {
        ESP_LOGI(TAG, "에어컨 팬 속도 증가 명령 실행 (Arduino 검증 데이터)");
        sendArduinoRawData(fan_faster_cmd, sizeof(fan_faster_cmd)/sizeof(fan_faster_cmd[0]));
        return "에어컨 팬 속도 증가 명령 전송 완료";
    } else if (command == "ac_fan_slower") {
        ESP_LOGI(TAG, "에어컨 팬 속도 감소 명령 실행 (Arduino 검증 데이터)");
        sendArduinoRawData(fan_slower_cmd, sizeof(fan_slower_cmd)/sizeof(fan_slower_cmd[0]));
        return "에어컨 팬 속도 감소 명령 전송 완료";
    } else if (command == "ac_cool") {
        ESP_LOGI(TAG, "에어컨 냉방 모드 명령 실행 (Arduino 검증 데이터)");
        sendArduinoRawData(cool_cmd, sizeof(cool_cmd)/sizeof(cool_cmd[0]));
        return "에어컨 냉방 모드 명령 전송 완료";
    } else if (command == "ac_energy") {
        ESP_LOGI(TAG, "에어컨 절전 모드 명령 실행 (Arduino 검증 데이터)");
        sendArduinoRawData(energy_cmd, sizeof(energy_cmd)/sizeof(energy_cmd[0]));
        return "에어컨 절전 모드 명령 전송 완료";
    } else if (command == "ac_fan_only") {
        ESP_LOGI(TAG, "에어컨 송풍 전용 모드 명령 실행 (Arduino 검증 데이터)");
        sendArduinoRawData(fan_only_cmd, sizeof(fan_only_cmd)/sizeof(fan_only_cmd[0]));
        return "에어컨 송풍 전용 모드 명령 전송 완료";
    } else if (command == "ac_sleep") {
        ESP_LOGI(TAG, "에어컨 수면 모드 명령 실행 (Arduino 검증 데이터)");
        sendArduinoRawData(sleep_cmd, sizeof(sleep_cmd)/sizeof(sleep_cmd[0]));
        return "에어컨 수면 모드 명령 전송 완료";
    } else if (command == "ac_auto") {
        ESP_LOGI(TAG, "에어컨 자동 모드 명령 실행 (Arduino 검증 데이터)");
        sendArduinoRawData(auto_cmd, sizeof(auto_cmd)/sizeof(auto_cmd[0]));
        return "에어컨 자동 모드 명령 전송 완료";
    } else if (command == "ac_timer") {
        ESP_LOGI(TAG, "에어컨 타이머 명령 실행 (Arduino 검증 데이터)");
        sendArduinoRawData(timer_cmd, sizeof(timer_cmd)/sizeof(timer_cmd[0]));
        return "에어컨 타이머 명령 전송 완료";
    } else if (command == "ac_test") {
        ESP_LOGI(TAG, "에어컨 전체 테스트 시작");
        DynamicJsonDocument response_doc(1024);
        JsonArray results = response_doc.createNestedArray("results");

        // Arduino 검증된 에어컨 테스트 명령어들
        struct TestCommand {
            std::string name;
            const uint16_t* data;
            size_t length;
        };

        std::vector<TestCommand> test_commands = {
            {"power", power_cmd, sizeof(power_cmd)/sizeof(power_cmd[0])},
            {"temp_up", temp_up_cmd, sizeof(temp_up_cmd)/sizeof(temp_up_cmd[0])},
            {"temp_down", temp_down_cmd, sizeof(temp_down_cmd)/sizeof(temp_down_cmd[0])},
            {"fan_faster", fan_faster_cmd, sizeof(fan_faster_cmd)/sizeof(fan_faster_cmd[0])},
            {"fan_slower", fan_slower_cmd, sizeof(fan_slower_cmd)/sizeof(fan_slower_cmd[0])},
            {"cool", cool_cmd, sizeof(cool_cmd)/sizeof(cool_cmd[0])},
            {"energy", energy_cmd, sizeof(energy_cmd)/sizeof(energy_cmd[0])},
            {"fan_only", fan_only_cmd, sizeof(fan_only_cmd)/sizeof(fan_only_cmd[0])},
            {"sleep", sleep_cmd, sizeof(sleep_cmd)/sizeof(sleep_cmd[0])},
            {"auto", auto_cmd, sizeof(auto_cmd)/sizeof(auto_cmd[0])},
            {"timer", timer_cmd, sizeof(timer_cmd)/sizeof(timer_cmd[0])}
        };

        for (const auto& cmd : test_commands) {
            JsonObject result = results.createNestedObject();
            result["command"] = cmd.name.c_str();
            result["data_length"] = cmd.length;

            ESP_LOGI(TAG, "Arduino 검증 에어컨 테스트: %s (%d pulses)", cmd.name.c_str(), (int)cmd.length);
            sendArduinoRawData(cmd.data, cmd.length);
            result["success"] = true;
            result["message"] = "Arduino 검증 데이터 전송 완료";

            vTaskDelay(pdMS_TO_TICKS(1000)); // 1초 대기
        }

        response_doc["total_tests"] = test_commands.size();
        response_doc["library"] = "IRremoteESP8266 + Arduino 검증 데이터";
        response_doc["frequency"] = "38kHz";

        std::string result_str;
        serializeJson(response_doc, result_str);
        return result_str;
    } else if (command == "irremote_test") {
        ESP_LOGI(TAG, "IRremoteESP8266 라이브러리 테스트 시작");
        if (g_irremote_sender) {
            // Samsung TV 전원 코드 테스트
            g_irremote_sender->sendNEC(0x20DF10EF);
            ESP_LOGI(TAG, "Samsung TV 전원 코드 전송 완료");

            vTaskDelay(pdMS_TO_TICKS(1000));

            // Samsung TV 볼륨 업 코드 테스트
            g_irremote_sender->sendNEC(0x20DF40BF);
            ESP_LOGI(TAG, "Samsung TV 볼륨 업 코드 전송 완료");

            return "IRremoteESP8266 라이브러리 테스트 완료";
        } else {
            return "오류: IRremoteESP8266 송신기가 초기화되지 않음";
        }
    } else if (command == "led_test") {
        ESP_LOGI(TAG, "GPIO 25번 핀 LED 테스트 시작");

        // GPIO 25번 핀 설정 (IRremoteESP8266와 충돌 방지를 위해)
        gpio_config_t io_conf = {};
        io_conf.intr_type = GPIO_INTR_DISABLE;
        io_conf.mode = GPIO_MODE_OUTPUT;
        io_conf.pin_bit_mask = (1ULL << 25);
        io_conf.pull_down_en = GPIO_PULLDOWN_DISABLE;
        io_conf.pull_up_en = GPIO_PULLUP_DISABLE;
        gpio_config(&io_conf);

        // 5번 깜빡이기 (더 긴 시간으로)
        for (int i = 0; i < 5; i++) {
            gpio_set_level(GPIO_NUM_25, 1);
            ESP_LOGI(TAG, "LED ON - %d/5 ", i+1);
            vTaskDelay(pdMS_TO_TICKS(1000)); // 1초간 켜기
            gpio_set_level(GPIO_NUM_25, 0);
            ESP_LOGI(TAG, "LED OFF - %d/5", i+1);
            vTaskDelay(pdMS_TO_TICKS(500));
        }

        return "GPIO 25번 핀 LED 테스트 완료 (1초간 켜짐)";
    } else if (command == "ir_status") {
        ESP_LOGI(TAG, "IR 송신기 상태 확인");
        DynamicJsonDocument status_doc(512);

        status_doc["irremote_sender"] = (g_irremote_sender != nullptr) ? "초기화됨" : "초기화 안됨";
        status_doc["ir_sender"] = (g_ir_sender != nullptr) ? "초기화됨" : "초기화 안됨";
        status_doc["gpio_pin"] = 25;
        status_doc["library"] = "IRremoteESP8266";

        if (g_irremote_sender) {
            // IRremoteESP8266 라이브러리로 간단한 테스트 신호 전송
            ESP_LOGI(TAG, "IRremoteESP8266 라이브러리 테스트 신호 전송");
            g_irremote_sender->sendNEC(0x20DF10EF);
            status_doc["test_signal"] = "전송됨";
        } else {
            status_doc["test_signal"] = "송신기 없음";
        }

        std::string result_str;
        serializeJson(status_doc, result_str);
        return result_str;
    } else if (command == "simple_test") {
        ESP_LOGI(TAG, "간단한 IR 테스트 시작");
        if (g_irremote_sender) {
            // 매우 간단한 Raw 데이터로 테스트
            uint16_t simple_raw[] = {1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000};
            g_irremote_sender->sendRaw(simple_raw, 8, 38);
            ESP_LOGI(TAG, "간단한 Raw 데이터 전송 완료");

            vTaskDelay(pdMS_TO_TICKS(1000));

            // NEC 코드로도 테스트
            g_irremote_sender->sendNEC(0x20DF10EF);
            ESP_LOGI(TAG, "NEC 코드 전송 완료");

            return "간단한 IR 테스트 완료";
        } else {
            return "오류: IRremoteESP8266 송신기가 초기화되지 않음";
        }
    } else if (command == "hardware_test") {
        ESP_LOGI(TAG, "하드웨어 종합 테스트 시작");

        // 1. GPIO 25번 핀 직접 제어 테스트
        ESP_LOGI(TAG, "1단계: GPIO 25번 핀 직접 제어 테스트");
        gpio_config_t io_conf = {};
        io_conf.intr_type = GPIO_INTR_DISABLE;
        io_conf.mode = GPIO_MODE_OUTPUT;
        io_conf.pin_bit_mask = (1ULL << 25);
        io_conf.pull_down_en = GPIO_PULLDOWN_DISABLE;
        io_conf.pull_up_en = GPIO_PULLUP_DISABLE;
        gpio_config(&io_conf);

        gpio_set_level(GPIO_NUM_25, 1);
        ESP_LOGI(TAG, "GPIO 25 HIGH - 2초간 유지");
        vTaskDelay(pdMS_TO_TICKS(2000));
        gpio_set_level(GPIO_NUM_25, 0);
        ESP_LOGI(TAG, "GPIO 25 LOW");

        vTaskDelay(pdMS_TO_TICKS(1000));

        // 2. IRremoteESP8266 라이브러리 테스트
        ESP_LOGI(TAG, "2단계: IRremoteESP8266 라이브러리 테스트");
        if (g_irremote_sender) {
            g_irremote_sender->sendNEC(0x20DF10EF);
            ESP_LOGI(TAG, "NEC 코드 전송 완료");

            vTaskDelay(pdMS_TO_TICKS(1000));

            // 3. Arduino 검증 Raw 데이터 테스트
            ESP_LOGI(TAG, "3단계: Arduino 검증 Raw 데이터 테스트");
            sendArduinoRawData(power_cmd, sizeof(power_cmd)/sizeof(power_cmd[0]));
            ESP_LOGI(TAG, "Arduino 검증 데이터 전송 완료");
        }

        return "하드웨어 종합 테스트 완료";
    } else if (command == "mqtt_debug") {
        ESP_LOGI(TAG, "MQTT 디버깅 정보");
        DynamicJsonDocument debug_doc(1024);

        debug_doc["device_id"] = DEVICE_ID;
        debug_doc["mqtt_connected"] = g_pubsub_client.connected();
        debug_doc["mqtt_state"] = g_pubsub_client.state();
        debug_doc["wifi_connected"] = (WiFi.status() == WL_CONNECTED);
        debug_doc["wifi_ssid"] = WiFi.SSID().c_str();
        debug_doc["wifi_ip"] = WiFi.localIP().toString().c_str();
        debug_doc["control_topic"] = std::string("hub/") + std::string(DEVICE_ID) + "/order/control";

        // MQTT 상태 상세 정보
        const char* mqtt_state_str = "UNKNOWN";
        switch (g_pubsub_client.state()) {
            case MQTT_CONNECTION_TIMEOUT: mqtt_state_str = "CONNECTION_TIMEOUT"; break;
            case MQTT_CONNECTION_LOST: mqtt_state_str = "CONNECTION_LOST"; break;
            case MQTT_CONNECT_FAILED: mqtt_state_str = "CONNECT_FAILED"; break;
            case MQTT_DISCONNECTED: mqtt_state_str = "DISCONNECTED"; break;
            case MQTT_CONNECTED: mqtt_state_str = "CONNECTED"; break;
            case MQTT_CONNECT_BAD_PROTOCOL: mqtt_state_str = "CONNECT_BAD_PROTOCOL"; break;
            case MQTT_CONNECT_BAD_CLIENT_ID: mqtt_state_str = "CONNECT_BAD_CLIENT_ID"; break;
            case MQTT_CONNECT_UNAVAILABLE: mqtt_state_str = "CONNECT_UNAVAILABLE"; break;
            case MQTT_CONNECT_BAD_CREDENTIALS: mqtt_state_str = "CONNECT_BAD_CREDENTIALS"; break;
            case MQTT_CONNECT_UNAUTHORIZED: mqtt_state_str = "CONNECT_UNAUTHORIZED"; break;
        }
        debug_doc["mqtt_state_str"] = mqtt_state_str;

        // MQTT 재연결 시도
        if (!g_pubsub_client.connected()) {
            ESP_LOGI(TAG, "MQTT 재연결 시도 중...");
            // connectMQTT 함수는 나중에 정의되므로 여기서는 연결 상태만 확인
            debug_doc["reconnect_attempted"] = true;
            debug_doc["reconnect_success"] = false;
            debug_doc["note"] = "MQTT 재연결은 부팅 시에만 시도됩니다";
        }

        std::string result_str;
        serializeJson(debug_doc, result_str);
        return result_str;
    } else if (command == "mqtt_test_send") {
        ESP_LOGI(TAG, "MQTT 테스트 메시지 전송");

        if (!g_pubsub_client.connected()) {
            return "MQTT 연결 안됨";
        }

        // 테스트 응답 메시지 전송
        DynamicJsonDocument test_doc(512);
        test_doc["tx_id"] = 999;
        test_doc["status"] = "test_response";
        test_doc["message"] = "MQTT 수신 테스트 성공";
        test_doc["timestamp"] = millis();

        std::string test_message;
        serializeJson(test_doc, test_message);

        std::string response_topic = "hub/" + std::string(DEVICE_ID) + "/order/response";
        bool result = g_pubsub_client.publish(response_topic.c_str(), test_message.c_str());

        if (result) {
            ESP_LOGI(TAG, "테스트 메시지 전송 성공: %s", response_topic.c_str());
            return "테스트 메시지 전송 성공";
        } else {
            ESP_LOGE(TAG, "테스트 메시지 전송 실패");
            return "테스트 메시지 전송 실패";
        }
    } else if (command == "device_info") {
        ESP_LOGI(TAG, "디바이스 정보 확인");

        // 간단한 문자열 응답으로 변경
        std::string info = "Device ID: " + std::string(DEVICE_ID) +
                          "\nMQTT Control Topic: hub/" + std::string(DEVICE_ID) + "/order/control" +
                          "\nMQTT Response Topic: hub/" + std::string(DEVICE_ID) + "/order/response" +
                          "\nMQTT Error Topic: hub/" + std::string(DEVICE_ID) + "/error" +
                          "\nMQTT Connected: " + (g_pubsub_client.connected() ? "true" : "false") +
                          "\nWiFi Connected: " + (WiFi.status() == WL_CONNECTED ? "true" : "false");

        if (WiFi.status() == WL_CONNECTED) {
            info += "\nWiFi SSID: " + std::string(WiFi.SSID().c_str());
            info += "\nWiFi IP: " + std::string(WiFi.localIP().toString().c_str());
        }

        return info;
    } else if (command == "mqtt_resubscribe") {
        ESP_LOGI(TAG, "MQTT 구독 재시도");

        if (!g_pubsub_client.connected()) {
            return "MQTT 연결 안됨";
        }

        std::string control_topic = "hub/" + std::string(DEVICE_ID) + "/order/control";
        bool result = g_pubsub_client.subscribe(control_topic.c_str());

        if (result) {
            ESP_LOGI(TAG, "MQTT 구독 재시도 성공: %s", control_topic.c_str());
            return "MQTT 구독 재시도 성공: " + control_topic;
        } else {
            ESP_LOGE(TAG, "MQTT 구독 재시도 실패: %s", control_topic.c_str());
            return "MQTT 구독 재시도 실패";
        }
    } else if (command == "samsung_power") {
        if (g_irremote_sender) {
            ESP_LOGI(TAG, "Samsung TV 전원 코드 전송 시작: 0x20DF10EF");
            g_irremote_sender->sendNEC(0x20DF10EF);
            ESP_LOGI(TAG, "Samsung TV 전원 명령 전송 완료 (IRremoteESP8266)");

            // 추가 테스트: Raw 데이터로도 전송해보기
            ESP_LOGI(TAG, "추가 테스트: Raw 데이터로 Samsung TV 전원 코드 전송");
            uint16_t raw_data[] = {9000, 4500, 560, 560, 560, 560, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 560, 560, 1690, 560, 560, 560, 1690, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560};
            g_irremote_sender->sendRaw(raw_data, sizeof(raw_data)/sizeof(raw_data[0]), 38);
            ESP_LOGI(TAG, "Raw 데이터 전송 완료");

            return "Samsung TV 전원 명령 전송 완료 (NEC + Raw)";
        } else {
            return "오류: IRremoteESP8266 송신기가 초기화되지 않음";
        }
    } else if (command == "samsung_volume_up") {
        if (g_irremote_sender) {
            g_irremote_sender->sendNEC(0x20DF40BF);
            ESP_LOGI(TAG, "Samsung TV 볼륨 업 명령 전송 완료 (IRremoteESP8266)");
            return "Samsung TV 볼륨 업 명령 전송 완료";
        } else {
            return "오류: IRremoteESP8266 송신기가 초기화되지 않음";
        }
    } else if (command == "samsung_volume_down") {
        if (g_irremote_sender) {
            g_irremote_sender->sendNEC(0x20DFC03F);
            ESP_LOGI(TAG, "Samsung TV 볼륨 다운 명령 전송 완료 (IRremoteESP8266)");
            return "Samsung TV 볼륨 다운 명령 전송 완료";
        } else {
            return "오류: IRremoteESP8266 송신기가 초기화되지 않음";
        }
    } else if (command == "restart") {
        ESP_LOGI(TAG, "시스템 재시작 요청");
        vTaskDelay(pdMS_TO_TICKS(1000));
        esp_restart();
        return "재시작 중";
    }

    return "알 수 없는 명령어: " + command;
}

bool connectMQTT() {
    std::string mqtt_broker = "43.201.62.254";
    int mqtt_port = 8883;

    ESP_LOGI(TAG, "MQTT 설정값 확인 - 브로커: %s, 포트: %d", mqtt_broker.c_str(), mqtt_port);
    ESP_LOGI(TAG, "MQTT 연결 시도: %s:%d", mqtt_broker.c_str(), mqtt_port);

    if (WiFi.status() != WL_CONNECTED) {
        ESP_LOGE(TAG, "WiFi가 연결되지 않음. Arduino WiFi 상태: %d", WiFi.status());
        return false;
    }

    ESP_LOGI(TAG, "WiFi 연결 확인됨 - SSID: %s, IP: %s", WiFi.SSID().c_str(), WiFi.localIP().toString().c_str());


    ESP_LOGI(TAG, "TLS 설정 중...");

    g_secure_client.stop();
    g_secure_client.setInsecure();
    g_secure_client.setTimeout(20000);

    ESP_LOGI(TAG, "TLS 설정 완료");

    g_pubsub_client.setServer(mqtt_broker.c_str(), mqtt_port);
    g_pubsub_client.setBufferSize(1024);  // MQTT 버퍼 크기 설정 (기본값: 256)
    g_pubsub_client.setCallback([](char* topic, unsigned char* payload, unsigned int length) {
        onMQTTMessage(topic, payload, length);
    });

    int retry_count = 0;
    const int max_retries = 3;

    while (retry_count < max_retries) {
        ESP_LOGI(TAG, "MQTT 연결 시도 %d/%d", retry_count + 1, max_retries);
        std::string mqtt_client_id = MQTT_CLIENT_ID;
        ESP_LOGI(TAG, "브로커: %s, 포트: %d, 클라이언트 ID: %s", mqtt_broker.c_str(), mqtt_port, mqtt_client_id.c_str());

        if (WiFi.status() != WL_CONNECTED) {
            ESP_LOGE(TAG, "연결 시도 중 WiFi 연결 끊어짐. Arduino WiFi 상태: %d", WiFi.status());
            return false;
        }
        ESP_LOGI(TAG, "WiFi 연결 상태 재확인 - SSID: %s, RSSI: %d", WiFi.SSID().c_str(), WiFi.RSSI());

        std::string mqtt_username = "eeum";
        std::string mqtt_password = "ssafy2086eeum";

        if (g_pubsub_client.connect(mqtt_client_id.c_str(), mqtt_username.c_str(), mqtt_password.c_str())) {
            ESP_LOGI(TAG, "MQTT 연결 성공!");

            std::string control_topic = "hub/" + std::string(DEVICE_ID) + "/order/control";

            g_pubsub_client.subscribe(control_topic.c_str());

            ESP_LOGI(TAG, "MQTT 토픽 구독: %s", control_topic.c_str());

            return true;
        } else {
            retry_count++;
            ESP_LOGE(TAG, "MQTT 연결 실패 (%d/%d)", retry_count, max_retries);
            ESP_LOGE(TAG, "연결 상태: %d", g_pubsub_client.state());
            ESP_LOGE(TAG, "WiFi 상태: %d", WiFi.status());

            if (retry_count < max_retries) {
                ESP_LOGI(TAG, "5초 후 재시도");
                delay(5000);  // 재시도 간격을 늘림
            }
        }
    }

    ESP_LOGE(TAG, "MQTT 연결 최종 실패");
    return false;
}

void mqtt_task(void* parameter) {
    ESP_LOGI(TAG, "MQTT task 시작");

    // WiFi 연결 대기 (단순화)
    for (int i = 0; i < 30; i++) {
        if (WiFi.status() == WL_CONNECTED) {
            ESP_LOGI(TAG, "WiFi 연결 확인됨. MQTT 연결 시작");
            break;
        }
        vTaskDelay(pdMS_TO_TICKS(1000));
    }

    vTaskDelay(pdMS_TO_TICKS(2000));

    if (connectMQTT()) {
        ESP_LOGI(TAG, "MQTT 연결 성공");
    } else {
        ESP_LOGW(TAG, "MQTT 연결 실패 - 시리얼 통신으로만 동작");
    }

    unsigned long last_reconnect = 0;

    while (true) {
        if (!g_pubsub_client.connected() && WiFi.status() == WL_CONNECTED) {
            if (millis() - last_reconnect > 5000) {
                last_reconnect = millis();
                connectMQTT();
            }
        }

        if (g_pubsub_client.connected()) {
            g_pubsub_client.loop();
        }

        vTaskDelay(pdMS_TO_TICKS(1000));
    }
}


void loadConfiguration() {
    ESP_LOGI(TAG, "설정 로드 중");

    auto config_from_file = Config::loadFromFile("/config/esp32_config.json");
    if (config_from_file) {
        g_config = new Config(*config_from_file);
        ESP_LOGI(TAG, "설정 파일에서 로드 완료");
    } else {
        g_config = new Config();
        g_config->setString("wifi.ssid", WIFI_SSID);
        g_config->setString("wifi.password", WIFI_PASSWORD);
        g_config->setString("mqtt.broker", "43.201.62.254");
        g_config->setInt("mqtt.port", 8883);
        g_config->setString("mqtt.client_id", "esp-1");
        g_config->setString("mqtt.username", "eeum");
        g_config->setString("mqtt.password", "ssafy2086eeum");
        ESP_LOGW(TAG, "설정 파일 로드 실패, 기본값 사용");

        ESP_LOGI(TAG, "설정된 MQTT 브로커: %s", g_config->getString("mqtt.broker", "").c_str());
        ESP_LOGI(TAG, "설정된 MQTT 포트: %d", g_config->getInt("mqtt.port", 0));
        ESP_LOGI(TAG, "설정된 MQTT 사용자명: %s", g_config->getString("mqtt.username", "").c_str());
    }

    ESP_LOGI(TAG, "설정 로드 완료");
}

void initHardware() {
    ESP_LOGI(TAG, "하드웨어 초기화 중");

    g_serial_controller = new SerialController(115200);
    g_serial_controller->setCommandCallback(onSerialCommand);
    g_serial_controller->setDebugMode(true);
    g_serial_controller->initialize();

    // IRremoteESP8266 라이브러리 초기화 (GPIO 25번 핀 사용 - DAC 핀, 더 강력!)
    g_irremote_sender = new IRsend(25);
    g_irremote_sender->begin();
    ESP_LOGI(TAG, "IRremoteESP8266 라이브러리 초기화 성공 (GPIO 25)");

    // 기존 IRSend도 유지 (호환성을 위해)
    g_ir_sender = new IRSend();
    bool ir_init_result = g_ir_sender->initialize();
    g_ir_sender->setDebugMode(true);

    if (ir_init_result) {
        ESP_LOGI(TAG, "RMT 방식 IR 송신기 초기화 성공");

        vTaskDelay(pdMS_TO_TICKS(3000));
        ESP_LOGI(TAG, "테스트 IR 신호 송신 ");

        // IRremoteESP8266 라이브러리로 테스트 신호 전송
        ESP_LOGI(TAG, "IRremoteESP8266 라이브러리 테스트 신호 전송");
        g_irremote_sender->sendNEC(0x20DF10EF); // Samsung TV 전원 코드
        ESP_LOGI(TAG, "테스트 IR 신호 전송 완료 (GPIO 25)");

        ESP_LOGI(TAG, "테스트 IR 신호 송신 완료");
    } else {
        ESP_LOGE(TAG, "RMT 방식 IR 송신기 초기화 실패");
    }


    // g_pubsub_client = new MqttClient();
    // g_pubsub_client->setMessageCallback(onMQTTMessage);

    // MqttClient::setGlobalInstance(g_pubsub_client);

    ESP_LOGI(TAG, "하드웨어 초기화 완료");
}

void createTasks() {
    ESP_LOGI(TAG, "task 생성 중");

    xTaskCreate(
        mqtt_task,
        "mqtt_task",
        8192,
        NULL,
        5,
        &mqtt_task_handle
    );

    ESP_LOGI(TAG, "task 생성 완료");
}

void setup() {
    ESP_LOGI(TAG, "ESP32 IR Remote 시작");
    ESP_LOGI(TAG, "모델: ESP32-WROOM-32E");
    ESP_LOGI(TAG, "Free heap: %d bytes", esp_get_free_heap_size());
    ESP_LOGI(TAG, "Chip revision: %d", esp_chip_info_t().revision);

    // if (Security::initialize()) {
    //     ESP_LOGI(TAG, "보안 시스템 초기화 성공");
    // } else {
    //     ESP_LOGE(TAG, "보안 시스템 초기화 실패");
    // }
    ESP_LOGI(TAG, "보안 시스템 초기화 건너뜀 (개발 중)");

    ESP_LOGI(TAG, "독립 실행 모드 활성화");

    // Serial.end();

    esp_err_t ret = nvs_flash_init();
    if (ret == ESP_ERR_NVS_NO_FREE_PAGES || ret == ESP_ERR_NVS_NEW_VERSION_FOUND) {
        ESP_ERROR_CHECK(nvs_flash_erase());
        ret = nvs_flash_init();
    }
    ESP_ERROR_CHECK(ret);

    loadConfiguration();

    initWiFi();

    initHardware();

    createTasks();

    ESP_LOGI(TAG, "ESP32 IR Remote 초기화 완료");

    // 독립 실행 모드 - 전력 절약 설정
    WiFi.setSleepMode(WIFI_LIGHT_SLEEP);
    setCpuFrequencyMhz(80);  // CPU 클럭 속도 조정 (240MHz → 80MHz)
    ESP_LOGI(TAG, "독립 실행 모드 - 전력 절약 활성화");

}

void loop() {
    if (g_serial_controller) {
        g_serial_controller->loop();
    }

    static uint32_t last_led_time = 0;
    static bool led_state = false;

    if (millis() - last_led_time > 2000) {
        led_state = !led_state;
        gpio_set_level(GPIO_NUM_2, led_state ? 1 : 0);
        last_led_time = millis();
    }

    static uint32_t last_wifi_check = 0;
    static uint32_t wifi_reconnect_count = 0;
    static uint32_t last_reconnect_time = 0;

    if (millis() - last_wifi_check > 10000) {
        wifi_ap_record_t ap_info;
        if (esp_wifi_sta_get_ap_info(&ap_info) != ESP_OK) {
            if (wifi_reconnect_count < 3 && (millis() - last_reconnect_time > 30000)) {
                ESP_LOGW(TAG, "WiFi 연결 끊어짐. 재연결 시도 %d/3...", wifi_reconnect_count + 1);

                esp_wifi_stop();
                vTaskDelay(pdMS_TO_TICKS(1000));
                esp_wifi_start();
                vTaskDelay(pdMS_TO_TICKS(2000));
                esp_wifi_connect();

                wifi_reconnect_count++;
                last_reconnect_time = millis();
            } else if (wifi_reconnect_count >= 3) {
                ESP_LOGE(TAG, "WiFi 재연결 시도 횟수 초과. 수동 재시작 필요");
            }
        } else {
            wifi_reconnect_count = 0;
            last_reconnect_time = 0;
        }
        last_wifi_check = millis();
    }

    delay(1000);
}
