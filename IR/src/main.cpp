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
#define MQTT_CLIENT_ID "esp32_ir_controller"
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
    ESP_LOGI(TAG, "MQTT 메시지 수신 - 토픽: %s, 내용: %s", topic, message.c_str());

    if (topic_str.find("/order/control") != std::string::npos) {
        ESP_LOGI(TAG, "IR 제어 명령 메시지 수신");

        DynamicJsonDocument doc(2048);
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
    g_pubsub_client.setCallback([](char* topic, unsigned char* payload, unsigned int length) {
        onMQTTMessage(topic, payload, length);
    });
    g_pubsub_client.setKeepAlive(60);
    g_pubsub_client.setSocketTimeout(15);

    int retry_count = 0;
    const int max_retries = 3;

    while (retry_count < max_retries) {
        ESP_LOGI(TAG, "MQTT 연결 시도 %d/%d", retry_count + 1, max_retries);
        std::string mqtt_client_id = g_config->getString("mqtt.client_id", "esp-1");
        ESP_LOGI(TAG, "브로커: %s, 포트: %d, 클라이언트 ID: %s", mqtt_broker.c_str(), mqtt_port, mqtt_client_id.c_str());

        if (WiFi.status() != WL_CONNECTED) {
            ESP_LOGE(TAG, "연결 시도 중 WiFi 연결 끊어짐. Arduino WiFi 상태: %d", WiFi.status());
            return false;
        }
        ESP_LOGI(TAG, "WiFi 연결 상태 재확인 - SSID: %s, RSSI: %d", WiFi.SSID().c_str(), WiFi.RSSI());

        std::string mqtt_username = "eeum";
        std::string mqtt_password = "ssafy2086eeum";

        ESP_LOGI(TAG, "MQTT 인증 정보 - 사용자명: %s, 비밀번호: %s",
                 mqtt_username.c_str(),
                 mqtt_password.empty() ? "(설정됨)" : "(설정됨)");

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

    g_ir_sender = new IRSend();
    bool ir_init_result = g_ir_sender->initialize();
    g_ir_sender->setDebugMode(true);

    if (ir_init_result) {
        ESP_LOGI(TAG, "IR 송신기 초기화 성공");

        vTaskDelay(pdMS_TO_TICKS(3000));
        ESP_LOGI(TAG, "테스트 IR 신호 송신 ");

        g_ir_sender->sendIRCode("0x20DF10EF");
        vTaskDelay(pdMS_TO_TICKS(1000));
        g_ir_sender->sendIRCode("0x20DF40BF");
        vTaskDelay(pdMS_TO_TICKS(1000));
        g_ir_sender->sendIRCode("0x20DFC03F");

        ESP_LOGI(TAG, "테스트 IR 신호 송신 완료");
    } else {
        ESP_LOGE(TAG, "IR 송신기 초기화 실패");
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
