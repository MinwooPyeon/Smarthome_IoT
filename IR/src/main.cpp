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
IRsend* g_irremote_sender = nullptr;



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

bool isSimpleMQTTCommand(const std::string& message);
void processSimpleMQTTCommand(const std::string& command);
std::string onSerialCommand(const std::string& command, const JsonObject& params);

void onMQTTMessage(char* topic, unsigned char* payload, unsigned int length) {
    std::string message((char*)payload, length);
    std::string topic_str = std::string(topic);
    ESP_LOGI(TAG, "=== MQTT 메시지 수신 ===");
    ESP_LOGI(TAG, "토픽: %s", topic);
    ESP_LOGI(TAG, "길이: %d bytes", length);
    ESP_LOGI(TAG, "내용: %s", message.c_str());
    ESP_LOGI(TAG, "===");

    if (length > 4096) {
        ESP_LOGE(TAG, "메시지가 너무 큼: %d bytes (최대 4096)", length);
        return;
    }

    if (topic_str.find("/order/control") != std::string::npos) {
        ESP_LOGI(TAG, "IR 제어 명령 메시지 수신");

        if (isSimpleMQTTCommand(message)) {
            ESP_LOGI(TAG, "단순 명령어 처리: %s", message.c_str());
            processSimpleMQTTCommand(message);
            return;
        }

        DynamicJsonDocument doc(4096);
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
                int value = item.as<int>();
                raw_data_array.push_back(value);
                if (i < 10) {
                    ESP_LOGI(TAG, "  [%d]: %d μs", i, value);
                }
            }
            i++;
        }

        if (raw_data.size() > 0) {
            int min_val = raw_data_array[0];
            int max_val = raw_data_array[0];
            long total_val = 0;
            int short_pulses = 0;
            int long_pulses = 0;
            int exact_values = 0;

            for (int val : raw_data_array) {
                if (val < min_val) min_val = val;
                if (val > max_val) max_val = val;
                total_val += val;

                if (val < 50) short_pulses++;
                if (val > 2000) long_pulses++;
                if (val % 5 == 0) exact_values++;
            }

            ESP_LOGI(TAG, "타이밍 분석 - 최소: %dμs, 최대: %dμs, 평균: %ldμs",
                     min_val, max_val, total_val / raw_data_array.size());
            ESP_LOGI(TAG, "펄스 분포 - 짧은펄스(<50μs): %d개, 긴펄스(>2000μs): %d개",
                     short_pulses, long_pulses);
            ESP_LOGI(TAG, "정밀도 - 5의배수값: %d개 (%.1f%%)",
                     exact_values, (float)exact_values * 100 / raw_data_array.size());
        }

        if (raw_data.size() > 10) {
            ESP_LOGI(TAG, "총 %d개 데이터", raw_data.size());
        }

        if (g_ir_sender && !raw_data_array.empty()) {
            auto start_time = esp_timer_get_time();

            gpio_set_level(GPIO_NUM_2, 1);
            ESP_LOGI(TAG, "LED ON - IR 송신 시작");

            ESP_LOGI(TAG, "=== IR 송신 디버그 정보 ===");
            ESP_LOGI(TAG, "Raw 데이터: %d개 펄스", (int)raw_data_array.size());
            ESP_LOGI(TAG, "첫 5개 타이밍: %d, %d, %d, %d, %d",
                     raw_data_array[0], raw_data_array[1], raw_data_array[2],
                     raw_data_array[3], raw_data_array[4]);
            ESP_LOGI(TAG, "GPIO 25번 핀에서 38kHz 캐리어로 송신 중...");
            ESP_LOGI(TAG, "===");

            auto result = g_ir_sender->sendRawData(raw_data_array);

            auto duration = (esp_timer_get_time() - start_time) / 1000;
            ESP_LOGI(TAG, "IR 송신 완료 (소요시간: %lldms, 결과: %s)", duration,
                     result.result == IRSendResult::SUCCESS ? "성공" : "실패");

            if (result.result != IRSendResult::SUCCESS) {
                ESP_LOGE(TAG, "IR 송신 실패 원인: %s", result.message.c_str());
            } else {
                ESP_LOGI(TAG, "IR 송신 성공 - 기기가 응답해야 함");
            }

            if (result.result == IRSendResult::SUCCESS) {
                ESP_LOGI(TAG, "IR 신호 지속시간 연장을 위한 반복 송신 시작");

                auto repeat_result = g_ir_sender->sendRepeatedSignal(raw_data_array, 20, 20);

                if (repeat_result.result == IRSendResult::SUCCESS) {
                    ESP_LOGI(TAG, "IR 신호 반복 송신 성공 - 총 6회 송신 완료");
                } else {
                    ESP_LOGW(TAG, "IR 신호 반복 송신 실패: %s", repeat_result.message.c_str());
                }
            }

            ESP_LOGI(TAG, "적외선 다이오드 2초간 연속 송신 시작...");
            auto continuous_result = g_ir_sender->sendContinuousSignal(2000);

            if (continuous_result.result == IRSendResult::SUCCESS) {
                ESP_LOGI(TAG, "적외선 다이오드 2초간 연속 송신 성공");
            } else {
                ESP_LOGW(TAG, "적외선 다이오드 연속 송신 실패: %s", continuous_result.message.c_str());
            }

            gpio_set_level(GPIO_NUM_2, 0);
            ESP_LOGI(TAG, "LED OFF - IR 송신 완료");

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

bool isSimpleMQTTCommand(const std::string& message) {
    static const std::vector<std::string> simple_commands = {
        "ac_both"
    };

    for (const auto& cmd : simple_commands) {
        if (message == cmd) {
            return true;
        }
    }

    return false;
}

void processSimpleMQTTCommand(const std::string& command) {
    ESP_LOGI(TAG, "MQTT 단순 명령어 실행: %s", command.c_str());

    gpio_set_level(GPIO_NUM_2, 1);

    std::string result = onSerialCommand(command, JsonObject());

    gpio_set_level(GPIO_NUM_2, 0);

    ESP_LOGI(TAG, "명령어 실행 결과: %s", result.c_str());

    std::string response_topic = "hub/" + std::string(DEVICE_ID) + "/response/command";
    DynamicJsonDocument response_doc(512);
    response_doc["command"] = command.c_str();
    response_doc["result"] = result.c_str();
    response_doc["timestamp"] = esp_timer_get_time() / 1000;

    std::string response_str;
    serializeJson(response_doc, response_str);
    g_pubsub_client.publish(response_topic.c_str(), response_str.c_str());

    ESP_LOGI(TAG, "응답 전송: %s", response_str.c_str());
}

std::string onSerialCommand(const std::string& command, const JsonObject& params) {
    ESP_LOGI(TAG, "시리얼 명령 수신: %s", command.c_str());

    if (command == "ac_both") {
        if (g_ir_sender) {
            ESP_LOGI(TAG, "삼성 에어컨 ON/OFF 연속 테스트 시작");

            gpio_set_level(GPIO_NUM_2, 1);
            ESP_LOGI(TAG, "LED ON - 에어컨 ON/OFF 연속 테스트");

            std::vector<int> samsung_ac_on_signal = {
                612,408,565,1429,561,409,612,408,561,409,620,408,561,408,612,408,561,460,561,1428,
                561,459,512,459,561,1428,561,1428,561,459,561,1428,561,1429,561,1428,561,1428,561,1428,
                561,459,510,510,510,459,510,510,510,459,510,510,510,459,510,510,510,459,561,459,
                561,408,561,459,561,411,561,459,561,408,561,459,561,408,561,459,561,408,561,459,
                561,459,510,459,561,459,510,510,510,459,510,510,510,459,561,459,510,459,561,459,
                510,459,561,459,510,459,561,459,510,1479,510,1479,510,2958,3060
            };

            std::vector<int> samsung_ac_off_signal = {
                561,408,612,1378,612,409,561,459,561,409,567,459,510,459,561,459,510,460,561,1479,
                510,459,561,459,510,1479,510,1479,561,408,561,1428,561,1430,561,1428,561,1437,561,1431,
                561,459,561,408,561,459,561,459,510,459,561,459,510,459,561,459,510,510,510,459,
                561,459,510,459,561,460,510,459,561,459,510,459,561,459,510,486,510,459,561,459,
                510,510,510,459,510,510,510,459,510,510,510,459,561,459,561,408,561,459,561,408,
                561,459,561,408,561,459,561,408,561,1479,510,1479,510,2958,3009
            };

            ESP_LOGI(TAG, "1. 삼성 에어컨 ON 신호 연속 송신 (5회)");
            auto result1 = g_ir_sender->sendRepeatedSignal(samsung_ac_on_signal, 5, 100);
            vTaskDelay(pdMS_TO_TICKS(800));

            ESP_LOGI(TAG, "2. 삼성 에어컨 OFF 신호 연속 송신 (5회)");
            auto result2 = g_ir_sender->sendRepeatedSignal(samsung_ac_off_signal, 5, 100);
            vTaskDelay(pdMS_TO_TICKS(800));

            ESP_LOGI(TAG, "3. 삼성 에어컨 ON 신호 추가 송신 (8회)");
            auto result3 = g_ir_sender->sendRepeatedSignal(samsung_ac_on_signal, 8, 50);
            vTaskDelay(pdMS_TO_TICKS(800));

            ESP_LOGI(TAG, "4. 삼성 에어컨 OFF 신호 추가 송신 (8회)");
            auto result4 = g_ir_sender->sendRepeatedSignal(samsung_ac_off_signal, 8, 50);

            gpio_set_level(GPIO_NUM_2, 0);
            ESP_LOGI(TAG, "LED OFF - 에어컨 ON/OFF 연속 테스트 완료");

            return std::string("삼성 에어컨 ON/OFF 테스트 완료 - ON:") + (result1.result == IRSendResult::SUCCESS ? "성공" : "실패") +
                   std::string(", OFF:") + (result2.result == IRSendResult::SUCCESS ? "성공" : "실패") +
                   std::string(", ON반복:") + (result3.result == IRSendResult::SUCCESS ? "성공" : "실패") +
                   std::string(", OFF반복:") + (result4.result == IRSendResult::SUCCESS ? "성공" : "실패");
        } else {
            return "IR 송신기가 초기화되지 않음";
        }
    } else if (command == "raw_send") {
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
        ir_doc["tx_pin"] = 25;
        ir_doc["carrier_freq"] = 38000;
        ir_doc["duty_cycle"] = 50;

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
        limits_doc["max_pulses"] = 512;
        limits_doc["json_buffer_size"] = 8192;
        limits_doc["mqtt_buffer_size"] = 1024;
        limits_doc["estimated_max_raw_data"] = 400;
        limits_doc["mqtt_max_message_size"] = 1024;

        std::string result_str;
        serializeJson(limits_doc, result_str);
        return result_str;
    } else if (command == "device_list") {
        DynamicJsonDocument device_doc(512);
        JsonArray devices = device_doc.createNestedArray("devices");

        JsonObject tv = devices.createNestedObject();
        tv["id"] = "samsung_tv";
        tv["name"] = "Samsung TV";
        tv["type"] = "tv";

        JsonObject ac = devices.createNestedObject();
        ac["id"] = "samsung_ac";
        ac["name"] = "Samsung AC";
        ac["type"] = "air_conditioner";

        std::string result_str;
        serializeJson(device_doc, result_str);
        return result_str;
    } else if (command == "analyze_raw") {
        if (!params.containsKey("raw_data") || !params["raw_data"].is<JsonArray>()) {
            return "오류: raw_data 배열이 필요";
        }

        JsonArray raw_data = params["raw_data"];
        DynamicJsonDocument analysis_doc(1024);
        JsonArray original = analysis_doc.createNestedArray("original");
        JsonArray processed = analysis_doc.createNestedArray("processed");
        JsonArray differences = analysis_doc.createNestedArray("differences");

        for (JsonVariant item : raw_data) {
            if (item.is<int>()) {
                int original_val = item.as<int>();
                int processed_val = original_val;

                if (processed_val < 50) processed_val = 50;
                if (processed_val > 65535) processed_val = 65535;
                if (processed_val % 10 != 0) {
                    processed_val = ((processed_val + 5) / 10) * 10;
                }

                original.add(original_val);
                processed.add(processed_val);
                differences.add(processed_val - original_val);
            }
        }

        analysis_doc["total_pulses"] = raw_data.size();
        analysis_doc["max_difference"] = 0;
        analysis_doc["avg_difference"] = 0.0;

        int total_diff = 0;
        int max_diff = 0;
        for (JsonVariant diff : differences) {
            int d = diff.as<int>();
            total_diff += abs(d);
            if (abs(d) > max_diff) max_diff = abs(d);
        }

        analysis_doc["max_difference"] = max_diff;
        analysis_doc["avg_difference"] = raw_data.size() > 0 ? (double)total_diff / raw_data.size() : 0.0;

        std::string result_str;
        serializeJson(analysis_doc, result_str);
        return result_str;
    } else if (command == "samsung_test") {
        if (g_ir_sender) {
            DynamicJsonDocument response_doc(512);
            JsonArray results = response_doc.createNestedArray("results");

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

                vTaskDelay(pdMS_TO_TICKS(500));
            }

            response_doc["total_tests"] = test_commands.size();
            response_doc["library"] = "RMT";

            std::string result_str;
            serializeJson(response_doc, result_str);
            return result_str;
        } else {
            return "오류: IR 송신기가 초기화되지 않음";
        }
    } else if (command == "test_ir") {
        if (g_ir_sender) {
            ESP_LOGI(TAG, "IR 송신 테스트 시작");

            std::vector<int> test_signal = {612,408,565,1429,561,409,612,408,561,409,620,408,561,408,612,408,561,460,561,1428};

            gpio_set_level(GPIO_NUM_2, 1);
            auto result = g_ir_sender->sendRawData(test_signal);
            gpio_set_level(GPIO_NUM_2, 0);

            DynamicJsonDocument test_doc(256);
            test_doc["success"] = (result.result == IRSendResult::SUCCESS);
            test_doc["message"] = result.message.c_str();
            test_doc["duration_ms"] = result.duration_ms;
            test_doc["tx_pin"] = 25;
            test_doc["carrier_freq"] = 38000;

            std::string result_str;
            serializeJson(test_doc, result_str);
            return result_str;
        } else {
            return "오류: IR 송신기가 초기화되지 않음";
        }
    } else if (command == "irremote_test") {
        ESP_LOGI(TAG, "IRremoteESP8266 라이브러리 테스트 시작");
        if (g_irremote_sender) {

            g_irremote_sender->sendNEC(0x20DF10EF);
            ESP_LOGI(TAG, "Samsung TV 전원 코드 전송 완료");

            vTaskDelay(pdMS_TO_TICKS(1000));


            g_irremote_sender->sendNEC(0x20DF40BF);
            ESP_LOGI(TAG, "Samsung TV 볼륨 업 코드 전송 완료");

            return "IRremoteESP8266 라이브러리 테스트 완료";
        } else {
            return "오류: IRremoteESP8266 송신기가 초기화되지 않음";
        }
    } else if (command == "led_test") {
        ESP_LOGI(TAG, "GPIO 25번 핀 LED 테스트 시작");


        gpio_config_t io_conf = {};
        io_conf.intr_type = GPIO_INTR_DISABLE;
        io_conf.mode = GPIO_MODE_OUTPUT;
        io_conf.pin_bit_mask = (1ULL << 25);
        io_conf.pull_down_en = GPIO_PULLDOWN_DISABLE;
        io_conf.pull_up_en = GPIO_PULLUP_DISABLE;
        gpio_config(&io_conf);

        for (int i = 0; i < 5; i++) {
            gpio_set_level(GPIO_NUM_25, 1);
            ESP_LOGI(TAG, "LED ON - %d/5 ", i+1);
            vTaskDelay(pdMS_TO_TICKS(1000));
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
            uint16_t simple_raw[] = {1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000};
            g_irremote_sender->sendRaw(simple_raw, 8, 38);
            ESP_LOGI(TAG, "간단한 Raw 데이터 전송 완료");

            vTaskDelay(pdMS_TO_TICKS(1000));

            g_irremote_sender->sendNEC(0x20DF10EF);
            ESP_LOGI(TAG, "NEC 코드 전송 완료");

            return "간단한 IR 테스트 완료";
        } else {
            return "오류: IRremoteESP8266 송신기가 초기화되지 않음";
        }
    } else if (command == "hardware_test") {
        ESP_LOGI(TAG, "하드웨어 종합 테스트 시작");

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

        ESP_LOGI(TAG, "2단계: IRremoteESP8266 라이브러리 테스트");
        if (g_irremote_sender) {
            g_irremote_sender->sendNEC(0x20DF10EF);
            ESP_LOGI(TAG, "NEC 코드 전송 완료");

            vTaskDelay(pdMS_TO_TICKS(1000));

        ESP_LOGI(TAG, "3단계: Raw 데이터 테스트 완료");
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

        if (!g_pubsub_client.connected()) {
            ESP_LOGI(TAG, "MQTT 재연결 시도 중...");
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


            return "Samsung TV 전원 명령 전송 완료 (NEC)";
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
    } else if (command == "analyze_raw") {
        if (!params.containsKey("raw_data") || !params["raw_data"].is<JsonArray>()) {
            return "오류: raw_data 배열이 필요";
        }

        JsonArray raw_data = params["raw_data"];
        DynamicJsonDocument analysis_doc(1024);
        JsonArray original = analysis_doc.createNestedArray("original");
        JsonArray processed = analysis_doc.createNestedArray("processed");
        JsonArray differences = analysis_doc.createNestedArray("differences");

        for (JsonVariant item : raw_data) {
            if (item.is<int>()) {
                int original_val = item.as<int>();
                int processed_val = original_val;

                if (processed_val < 50) processed_val = 50;
                if (processed_val > 65535) processed_val = 65535;
                if (processed_val % 10 != 0) {
                    processed_val = ((processed_val + 5) / 10) * 10;
                }

                original.add(original_val);
                processed.add(processed_val);
                differences.add(processed_val - original_val);
            }
        }

        analysis_doc["total_pulses"] = raw_data.size();
        analysis_doc["max_difference"] = 0;
        analysis_doc["avg_difference"] = 0.0;

        int total_diff = 0;
        int max_diff = 0;
        for (JsonVariant diff : differences) {
            int d = diff.as<int>();
            total_diff += abs(d);
            if (abs(d) > max_diff) max_diff = abs(d);
        }

        analysis_doc["max_difference"] = max_diff;
        analysis_doc["avg_difference"] = raw_data.size() > 0 ? (double)total_diff / raw_data.size() : 0.0;

        std::string result_str;
        serializeJson(analysis_doc, result_str);
        return result_str;
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
    g_pubsub_client.setBufferSize(32768);
    g_pubsub_client.setCallback([](char* topic, unsigned char* payload, unsigned int length) {
        onMQTTMessage(topic, payload, length);
    });
    g_pubsub_client.setKeepAlive(15);
    g_pubsub_client.setSocketTimeout(5);

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

            bool subscribe_result = g_pubsub_client.subscribe(control_topic.c_str());
            ESP_LOGI(TAG, "MQTT 토픽 구독: %s (결과: %s)", control_topic.c_str(), subscribe_result ? "성공" : "실패");

            if (subscribe_result) {
                ESP_LOGI(TAG, "MQTT 구독 성공 - 메시지 대기 중...");
            } else {
                ESP_LOGE(TAG, "MQTT 구독 실패!");
            }

            return true;
        } else {
            retry_count++;
            ESP_LOGE(TAG, "MQTT 연결 실패 (%d/%d)", retry_count, max_retries);
            ESP_LOGE(TAG, "연결 상태: %d", g_pubsub_client.state());
            ESP_LOGE(TAG, "WiFi 상태: %d", WiFi.status());

            if (retry_count < max_retries) {
                ESP_LOGI(TAG, "5초 후 재시도");
                delay(5000);
            }
        }
    }

    ESP_LOGE(TAG, "MQTT 연결 최종 실패");
    return false;
}

void mqtt_task(void* parameter) {
    ESP_LOGI(TAG, "MQTT task 시작");

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
                ESP_LOGI(TAG, "MQTT 재연결 시도...");
                connectMQTT();
            }
        }

        if (g_pubsub_client.connected()) {
            g_pubsub_client.loop();
        } else {
            static uint32_t last_warning = 0;
            if (millis() - last_warning > 10000) {
                ESP_LOGW(TAG, "MQTT 연결 끊어짐, 재연결 시도");
                last_warning = millis();
            }
        }

        vTaskDelay(pdMS_TO_TICKS(100));
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

    gpio_config_t led_conf = {};
    led_conf.intr_type = GPIO_INTR_DISABLE;
    led_conf.mode = GPIO_MODE_OUTPUT;
    led_conf.pin_bit_mask = (1ULL << 2);
    led_conf.pull_down_en = GPIO_PULLDOWN_DISABLE;
    led_conf.pull_up_en = GPIO_PULLUP_DISABLE;

    esp_err_t led_ret = gpio_config(&led_conf);
    if (led_ret == ESP_OK) {
        ESP_LOGI(TAG, "LED GPIO 2번 핀 초기화 성공");
        gpio_set_level(GPIO_NUM_2, 0);
    } else {
        ESP_LOGE(TAG, "LED GPIO 2번 핀 초기화 실패: %s", esp_err_to_name(led_ret));
    }

    g_serial_controller = new SerialController(115200);
    g_serial_controller->setCommandCallback(onSerialCommand);
    g_serial_controller->setDebugMode(true);
    g_serial_controller->initialize();

    g_irremote_sender = new IRsend(25);
    g_irremote_sender->begin();
    ESP_LOGI(TAG, "IRremoteESP8266 라이브러리 초기화 성공 (GPIO 25)");

    g_ir_sender = new IRSend();
    bool ir_init_result = g_ir_sender->initialize();
    g_ir_sender->setDebugMode(true);

    if (ir_init_result) {
        ESP_LOGI(TAG, "RMT 방식 IR 송신기 초기화 성공");

        vTaskDelay(pdMS_TO_TICKS(3000));
        ESP_LOGI(TAG, "테스트 IR 신호 송신 ");

        ESP_LOGI(TAG, "IRremoteESP8266 라이브러리 테스트 신호 전송");
        g_irremote_sender->sendNEC(0x20DF10EF); // Samsung TV 전원 코드
        ESP_LOGI(TAG, "테스트 IR 신호 전송 완료 (GPIO 25)");

        ESP_LOGI(TAG, "테스트 IR 신호 송신 완료");
    } else {
        ESP_LOGE(TAG, "RMT 방식 IR 송신기 초기화 실패");
    }


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

    ESP_LOGI(TAG, "보안 시스템 초기화 건너뜀 (개발 중)");

    ESP_LOGI(TAG, "독립 실행 모드 활성화");

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

    WiFi.setSleep(true);
    setCpuFrequencyMhz(240);
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
        ESP_LOGI(TAG, "LED 상태: %s (GPIO 2)", led_state ? "ON" : "OFF");
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
