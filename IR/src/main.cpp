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

// IR Remote 컴포넌트 포함
#include "core/config.h"
#include "network/mqtt_client.h"
#include "network/serial_controller.h"
#include "hardware/irsend.h"

static const char* TAG = "IR_REMOTE_MAIN";

// 전역 변수
Config* g_config = nullptr;
MqttClient* g_mqtt_client = nullptr;
SerialController* g_serial_controller = nullptr;
IRSend* g_ir_sender = nullptr;

// TLS MQTT 클라이언트
WiFiClientSecure g_secure_client;
PubSubClient g_mqtt_client_ssl(g_secure_client);

// WiFi 및 MQTT 설정은 platformio.ini에서 정의됨
// 매크로가 정의되지 않은 경우를 위한 기본값
#ifndef WIFI_SSID
#define WIFI_SSID "default_wifi"
#endif
#ifndef WIFI_PASSWORD
#define WIFI_PASSWORD "default_password"
#endif
#ifndef MQTT_BROKER
#define MQTT_BROKER "localhost"
#endif
#ifndef MQTT_PORT
#define MQTT_PORT 1883
#endif
#ifndef MQTT_CLIENT_ID
#define MQTT_CLIENT_ID "esp32_ir_controller"
#endif
#ifndef DEVICE_ID
#define DEVICE_ID "esp32_ir_01"
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

// task 핸들
TaskHandle_t mqtt_task_handle = NULL;

// WiFi 이벤트 핸들러
static void wifi_event_handler(void* arg, esp_event_base_t event_base,
                              int32_t event_id, void* event_data) {
    if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_START) {
        ESP_LOGI(TAG, "WiFi Station 시작");
        // 자동 연결 비활성화 - initWiFi()에서 수동으로 연결
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

// WiFi 연결 함수
void initWiFi() {
    ESP_LOGI(TAG, "WiFi 초기화 시작");

    esp_err_t ret = esp_netif_init();
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "esp_netif_init 실패: %s", esp_err_to_name(ret));
        return;
    }

    ret = esp_event_loop_create_default();
    if (ret != ESP_OK && ret != ESP_ERR_INVALID_STATE) {
        ESP_LOGE(TAG, "esp_event_loop_create_default 실패: %s", esp_err_to_name(ret));
        return;
    }

    esp_netif_create_default_wifi_sta();

    wifi_init_config_t cfg = WIFI_INIT_CONFIG_DEFAULT();
    ret = esp_wifi_init(&cfg);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "esp_wifi_init 실패: %s", esp_err_to_name(ret));
        return;
    }

    ESP_LOGI(TAG, "WiFi 초기화 성공");

    // 이벤트 핸들러 등록
    esp_event_handler_register(WIFI_EVENT, ESP_EVENT_ANY_ID, &wifi_event_handler, NULL);
    esp_event_handler_register(IP_EVENT, IP_EVENT_STA_GOT_IP, &wifi_event_handler, NULL);

    wifi_config_t wifi_config = {};
    strcpy((char*)wifi_config.sta.ssid, WIFI_SSID);
    strcpy((char*)wifi_config.sta.password, WIFI_PASSWORD);
    wifi_config.sta.threshold.authmode = WIFI_AUTH_WPA2_PSK;
    wifi_config.sta.threshold.rssi = -80;  // RSSI 임계값을 -80으로 낮춤 (더 약한 신호도 허용)
    wifi_config.sta.pmf_cfg.capable = false;  // PMF 보안 비활성화 (호환성 향상)
    wifi_config.sta.pmf_cfg.required = false;

    esp_wifi_set_mode(WIFI_MODE_STA);
    esp_wifi_set_config(WIFI_IF_STA, &wifi_config);

    // ESP32-WROOM-32E 전력 관리 설정
    esp_wifi_set_ps(WIFI_PS_NONE);  // 전력 절약 모드 비활성화 (연결 안정성 향상)

    // iPhone 핫스팟 호환성을 위한 추가 설정
    esp_wifi_set_max_tx_power(78);  // 최대 전송 전력 설정 (20dBm)

    esp_err_t wifi_start_result = esp_wifi_start();
    if (wifi_start_result != ESP_OK) {
        ESP_LOGE(TAG, "WiFi 시작 실패: %s", esp_err_to_name(wifi_start_result));
        return;
    }

    ESP_LOGI(TAG, "WiFi 초기화 완료 대기");
    vTaskDelay(pdMS_TO_TICKS(2000)); // 2초 대기

    // WiFi 모듈 상태 확인
    wifi_mode_t wifi_mode;
    esp_wifi_get_mode(&wifi_mode);
    ESP_LOGI(TAG, "WiFi 모드: %d", wifi_mode);

    // WiFi 스캔으로 사용 가능한 네트워크 확인 (ESP32-WROOM-32E 최적화)
    ESP_LOGI(TAG, "WiFi 네트워크 스캔 (ESP32-WROOM-32E)");
    wifi_scan_config_t scan_config = {};
    scan_config.ssid = NULL;
    scan_config.bssid = NULL;
    scan_config.channel = 0;
    scan_config.show_hidden = true;
    scan_config.scan_type = WIFI_SCAN_TYPE_ACTIVE;
    scan_config.scan_time.active.min = 2000;  // ESP32-WROOM-32E용 2초
    scan_config.scan_time.active.max = 5000;  // ESP32-WROOM-32E용 5초

    // WiFi 스캔 전 모듈 상태 확인
    wifi_mode_t current_mode;
    esp_wifi_get_mode(&current_mode);
    ESP_LOGI(TAG, "스캔 전 WiFi 모드: %d", current_mode);

    // WiFi 스캔 재시도 로직 (더 효율적으로 개선)
    esp_err_t scan_result = ESP_FAIL;
    int scan_retry = 0;
    const int max_retries = 3;  // 3번으로 줄임

    while (scan_result != ESP_OK && scan_retry < max_retries) {
        ESP_LOGI(TAG, "WiFi 스캔 시도 %d/%d", scan_retry + 1, max_retries);
        scan_result = esp_wifi_scan_start(&scan_config, true);
        if (scan_result != ESP_OK) {
            ESP_LOGW(TAG, "WiFi 스캔 시작 실패 (시도 %d/%d): %s", scan_retry + 1, max_retries, esp_err_to_name(scan_result));

            // 재시도 간격을 점진적으로 증가
            int delay_ms = 1000 + (scan_retry * 1000); // 1초, 2초, 3초
            vTaskDelay(pdMS_TO_TICKS(delay_ms));
            scan_retry++;
        }
    }

    if (scan_result != ESP_OK) {
        ESP_LOGE(TAG, "WiFi 스캔 최종 실패: %s", esp_err_to_name(scan_result));
    }

    uint16_t ap_count = 0;
    esp_wifi_scan_get_ap_num(&ap_count);
    ESP_LOGI(TAG, "발견된 WiFi 네트워크: %d개", ap_count);

    if (ap_count > 0) {
        wifi_ap_record_t ap_records[ap_count];
        esp_wifi_scan_get_ap_records(&ap_count, ap_records);

        for (int i = 0; i < ap_count; i++) {
            ESP_LOGI(TAG, "WiFi: %s (RSSI: %d, Auth: %d, Channel: %d)",
                     ap_records[i].ssid, ap_records[i].rssi, ap_records[i].authmode, ap_records[i].primary);
        }
    } else {
        ESP_LOGW(TAG, "WiFi 네트워크를 찾을 수 없습니다. iPhone 핫스팟을 확인해주세요.");
    }

    ESP_LOGI(TAG, "WiFi 연결 시도");
    ESP_LOGI(TAG, "SSID: %s", WIFI_SSID);

    // WiFi 스캔 완료 후 연결 시도
    esp_wifi_connect();

    // 연결 대기 (최대 10초)
    ESP_LOGI(TAG, "WiFi 연결 대기");
    vTaskDelay(pdMS_TO_TICKS(10000)); // 10초 대기

    // 연결 상태 확인
    wifi_ap_record_t ap_info;
    esp_err_t ap_info_result = esp_wifi_sta_get_ap_info(&ap_info);
    if (ap_info_result == ESP_OK) {
        ESP_LOGI(TAG, "WiFi 연결 성공!");
        ESP_LOGI(TAG, "연결된 SSID: %s", ap_info.ssid);
        ESP_LOGI(TAG, "RSSI: %d dBm", ap_info.rssi);
        ESP_LOGI(TAG, "인증 모드: %d", ap_info.authmode);
        ESP_LOGI(TAG, "채널: %d", ap_info.primary);
    } else {
        ESP_LOGW(TAG, "WiFi 연결 실패: %s", esp_err_to_name(ap_info_result));

        // 연결 실패 원인 분석
        wifi_ap_record_t ap_records[10];
        uint16_t ap_count_check = 10;
        esp_wifi_scan_get_ap_records(&ap_count_check, ap_records);

        bool target_found = false;
        for (int i = 0; i < ap_count_check; i++) {
            if (strcmp((char*)ap_records[i].ssid, WIFI_SSID) == 0) {
                ESP_LOGI(TAG, "목표 네트워크 발견: %s (RSSI: %d, Auth: %d)",
                         ap_records[i].ssid, ap_records[i].rssi, ap_records[i].authmode);
                target_found = true;
                break;
            }
        }

        if (!target_found) {
            ESP_LOGE(TAG, "목표 네트워크 '%s'를 찾을 수 없음", WIFI_SSID);
        } else {
            ESP_LOGW(TAG, "네트워크는 발견되었지만 연결에 실패, 재시도");
            esp_wifi_connect();
        }
    }
}


// 오류 메시지 전송 함수 (에러 타입 + 에러 메시지)
void sendErrorMessage(int tx_id, const std::string& error_type, const std::string& error_message) {
    DynamicJsonDocument error_doc(512);
    error_doc["tx_id"] = tx_id;
    error_doc["error"] = error_type.c_str();
    error_doc["message"] = error_message.c_str();

    std::string error_str;
    serializeJson(error_doc, error_str);
    std::string error_topic = "hub/" + std::string(DEVICE_ID) + "/error";

    if (g_mqtt_client_ssl.connected()) {
        g_mqtt_client_ssl.publish(error_topic.c_str(), error_str.c_str());
        ESP_LOGI(TAG, "오류 메시지 전송: %s", error_str.c_str());
    }
}

// 응답 메시지 전송 함수
void sendAckMessage(const std::string& msgId, const std::string& corrId,
                   const std::string& status, const std::string& detail,
                   int durationMs = 0, int retries = 0) {
    DynamicJsonDocument ack_doc(1024);
    ack_doc["ts"] = esp_timer_get_time() / 1000;
    ack_doc["deviceId"] = DEVICE_ID;
    ack_doc["schema"] = "ack/1.0";
    ack_doc["corrId"] = corrId.c_str();
    ack_doc["msgId"] = msgId.c_str();
    ack_doc["status"] = status.c_str();

    JsonObject result = ack_doc.createNestedObject("result");
    result["code"] = 0;
    result["detail"] = detail.c_str();

    ack_doc["durationMs"] = durationMs;
    ack_doc["retries"] = retries;

    std::string ack_str;
    serializeJson(ack_doc, ack_str);
    std::string ack_topic = "hub/" + std::string(DEVICE_ID) + "/order/ack";

    if (g_mqtt_client_ssl.connected()) {
        g_mqtt_client_ssl.publish(ack_topic.c_str(), ack_str.c_str());
        ESP_LOGI(TAG, "응답 메시지 전송: %s", ack_str.c_str());
    }
}

// 에러 메시지 전송 함수
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

    if (g_mqtt_client_ssl.connected()) {
        g_mqtt_client_ssl.publish(error_topic.c_str(), error_str.c_str());
        ESP_LOGI(TAG, "에러 메시지 전송: %s", error_str.c_str());
    }
}

// MQTT 메시지 콜백 (새로운 스키마용)
void onMQTTMessage(char* topic, unsigned char* payload, unsigned int length) {
    std::string message((char*)payload, length);
    std::string topic_str = std::string(topic);
    ESP_LOGI(TAG, "MQTT 메시지 수신 - 토픽: %s, 내용: %s", topic, message.c_str());

    // hub/{deviceId}/order/control 토픽 처리
    if (topic_str.find("/order/control") != std::string::npos) {
        ESP_LOGI(TAG, "IR 제어 명령 메시지 수신");

        // JSON 파싱
        DynamicJsonDocument doc(2048);
        DeserializationError error = deserializeJson(doc, message);
        if (error) {
            ESP_LOGE(TAG, "JSON 파싱 실패: %s", error.c_str());
            // tx_id를 추출할 수 없으므로 -1로 설정
            sendErrorMessage(-1, "INVALID_COMMAND", "JSON 파싱 실패 - 잘못된 JSON 형식입니다");
            return;
        }

        // 필수 필드 확인
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

        // 메타데이터 로깅
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

        // Raw 데이터 처리
        JsonArray raw_data = doc["raw_data"];
        std::vector<int> raw_data_array;

        ESP_LOGI(TAG, "Raw 데이터 (%d개):", raw_data.size());
        int i = 0;
        for (JsonVariant item : raw_data) {
            if (item.is<int>()) {
                raw_data_array.push_back(item.as<int>());
                if (i < 10) { // 처음 10개만 로깅
                    ESP_LOGI(TAG, "  [%d]: %d", i, item.as<int>());
                }
            }
            i++;
        }

        if (raw_data.size() > 10) {
            ESP_LOGI(TAG, "  ... (총 %d개 데이터)", raw_data.size());
        }

        // IR 신호 송신
        if (g_ir_sender && !raw_data_array.empty()) {
            auto start_time = esp_timer_get_time();

            ESP_LOGI(TAG, "IR 신호 송신 시작 (Raw 데이터: %d개 펄스)", (int)raw_data_array.size());
            auto result = g_ir_sender->sendRawData(raw_data_array);

            auto duration = (esp_timer_get_time() - start_time) / 1000;
            ESP_LOGI(TAG, "IR 송신 완료 (소요시간: %lldms, 결과: %s)", duration,
                     result.result == IRSendResult::SUCCESS ? "성공" : "실패");

            // IR 전송 결과에 따른 응답 처리
            if (result.result == IRSendResult::SUCCESS) {
                // 성공 응답 전송
                if (g_mqtt_client_ssl.connected()) {
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
                    g_mqtt_client_ssl.publish(response_topic.c_str(), response_str.c_str());
                    ESP_LOGI(TAG, "성공 응답 메시지 전송: %s", response_str.c_str());
                }
            } else {
                // IR 전송 실패 시 오류 메시지 전송
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

// 시리얼 명령 처리 콜백
std::string onSerialCommand(const std::string& command, const JsonObject& params) {
    ESP_LOGI(TAG, "시리얼 명령 수신: %s", command.c_str());

    if (command == "raw_send") {
        if (!params.containsKey("raw_data") || !params["raw_data"].is<JsonArray>()) {
            return "오류: raw_data 배열이 필요";
        }

        // Raw 데이터 배열 처리
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
        // IR 송신 상태 반환
        DynamicJsonDocument ir_doc(256);
        ir_doc["sending"] = (g_ir_sender != nullptr);
        ir_doc["tx_pin"] = 23;

        std::string result_str;
        serializeJson(ir_doc, result_str);
        return result_str;
    } else if (command == "mqtt_status") {
        DynamicJsonDocument mqtt_doc(256);
        mqtt_doc["connected"] = (g_mqtt_client && g_mqtt_client->isConnected());
        mqtt_doc["broker"] = g_config ? g_config->getString("mqtt.broker", "").c_str() : "";
        mqtt_doc["port"] = g_config ? g_config->getInt("mqtt.port", 1883) : 1883;

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

// TLS MQTT 연결 함수
bool connectMQTT() {
    ESP_LOGI(TAG, "TLS MQTT 연결 시도: %s:%d", MQTT_BROKER, MQTT_PORT);

    // WiFi 연결 상태 확인 (더 정확한 방법 사용)
    wifi_ap_record_t ap_info;
    esp_err_t wifi_status = esp_wifi_sta_get_ap_info(&ap_info);

    if (wifi_status != ESP_OK) {
        ESP_LOGE(TAG, "WiFi가 연결되지 않음. esp_wifi_sta_get_ap_info 결과: %s", esp_err_to_name(wifi_status));
        ESP_LOGE(TAG, "WiFi.status(): %d", WiFi.status());
        return false;
    }

    ESP_LOGI(TAG, "WiFi 연결됨. IP: %s", WiFi.localIP().toString().c_str());
    ESP_LOGI(TAG, "연결된 SSID: %s, RSSI: %d dBm", ap_info.ssid, ap_info.rssi);

    // TLS 클라이언트 설정
    g_secure_client.setInsecure(); // 인증서 검증 비활성화 (개발 환경용)
    g_secure_client.setTimeout(10000); // 10초 타임아웃 설정

    // MQTT 클라이언트 설정
    g_mqtt_client_ssl.setServer(MQTT_BROKER, MQTT_PORT);
    g_mqtt_client_ssl.setCallback([](char* topic, unsigned char* payload, unsigned int length) {
        onMQTTMessage(topic, payload, length);
    });
    g_mqtt_client_ssl.setKeepAlive(60);
    g_mqtt_client_ssl.setSocketTimeout(10);

    // 연결 시도 (최대 3회 재시도)
    int retry_count = 0;
    const int max_retries = 3;

    while (retry_count < max_retries) {
        ESP_LOGI(TAG, "TLS MQTT 연결 시도 %d/%d", retry_count + 1, max_retries);

        if (g_mqtt_client_ssl.connect(MQTT_CLIENT_ID, MQTT_USERNAME, MQTT_PASSWORD)) {
            ESP_LOGI(TAG, "TLS MQTT 연결 성공!");

            // 새로운 토픽 구조로 구독
            std::string control_topic = "hub/" + std::string(DEVICE_ID) + "/order/control";

            g_mqtt_client_ssl.subscribe(control_topic.c_str());

            ESP_LOGI(TAG, "MQTT 토픽 구독: %s", control_topic.c_str());

            return true;
        } else {
            retry_count++;
            ESP_LOGE(TAG, "TLS MQTT 연결 실패 (%d/%d)", retry_count, max_retries);

            if (retry_count < max_retries) {
                ESP_LOGI(TAG, "3초 후 재시도");
                delay(3000);
            }
        }
    }

    ESP_LOGE(TAG, "TLS MQTT 연결 최종 실패");
    return false;
}

// MQTT task
void mqtt_task(void* parameter) {
    // WiFi 연결 완료까지 대기
    ESP_LOGI(TAG, "MQTT task 시작 - WiFi 연결 대기 중");

    // WiFi 연결 완료까지 최대 30초 대기
    int wifi_wait_count = 0;
    const int max_wifi_wait = 30;

    while (wifi_wait_count < max_wifi_wait) {
        wifi_ap_record_t ap_info;
        esp_err_t wifi_status = esp_wifi_sta_get_ap_info(&ap_info);

        if (wifi_status == ESP_OK) {
            ESP_LOGI(TAG, "WiFi 연결 확인됨. MQTT 연결 시작");
            break;
        }

        ESP_LOGI(TAG, "WiFi 연결 대기 중... (%d/%d)", wifi_wait_count + 1, max_wifi_wait);
        vTaskDelay(pdMS_TO_TICKS(1000));
        wifi_wait_count++;
    }

    if (wifi_wait_count >= max_wifi_wait) {
        ESP_LOGE(TAG, "WiFi 연결 대기 시간 초과. MQTT task 종료");
        vTaskDelete(NULL);
        return;
    }

    while (true) {
        if (!g_mqtt_client_ssl.connected()) {
            ESP_LOGI(TAG, "[IR_REMOTE_MAIN] TLS MQTT 재연결 시도");

            if (connectMQTT()) {
                ESP_LOGI(TAG, "[IR_REMOTE_MAIN] TLS MQTT 연결 성공");
            } else {
                ESP_LOGE(TAG, "[IR_REMOTE_MAIN] TLS MQTT 연결 실패");
            }
        } else {
            // MQTT 루프 처리
            g_mqtt_client_ssl.loop();
        }

        vTaskDelay(pdMS_TO_TICKS(1000));
    }
}

// IR 수신 task (송신만 필요하므로 제거)

// 설정 로드
void loadConfiguration() {
    ESP_LOGI(TAG, "설정 로드 중");

    // 기본 설정 생성
    g_config = new Config();

    // ESP32 전용 설정 적용
    g_config->setString("wifi.ssid", WIFI_SSID);
    g_config->setString("wifi.password", WIFI_PASSWORD);
    g_config->setString("mqtt.broker", MQTT_BROKER);
    g_config->setInt("mqtt.port", MQTT_PORT);
    g_config->setString("mqtt.client_id", MQTT_CLIENT_ID);

    ESP_LOGI(TAG, "설정 로드 완료");
}

// 하드웨어 초기화
void initHardware() {
    ESP_LOGI(TAG, "하드웨어 초기화 중");

    // 시리얼 컨트롤러 초기화 (USB-C 연결)
    g_serial_controller = new SerialController(115200);
    g_serial_controller->setCommandCallback(onSerialCommand);
    g_serial_controller->setDebugMode(true);
    g_serial_controller->initialize();

    // IR 송신기 초기화 (GPIO 22번)
    g_ir_sender = new IRSend();
    bool ir_init_result = g_ir_sender->initialize();
    g_ir_sender->setDebugMode(true);

    if (ir_init_result) {
        ESP_LOGI(TAG, "IR 송신기 초기화 성공");

        // 테스트용 IR 신호 송신 (초기화 후 3초 뒤)
        vTaskDelay(pdMS_TO_TICKS(3000));
        ESP_LOGI(TAG, "테스트 IR 신호 송신 ");

        // 여러 테스트 코드 송신
        g_ir_sender->sendIRCode("0x20DF10EF"); // 전원 버튼
        vTaskDelay(pdMS_TO_TICKS(1000));
        g_ir_sender->sendIRCode("0x20DF40BF"); // 채널 업
        vTaskDelay(pdMS_TO_TICKS(1000));
        g_ir_sender->sendIRCode("0x20DFC03F"); // 채널 다운

        ESP_LOGI(TAG, "테스트 IR 신호 송신 완료");
    } else {
        ESP_LOGE(TAG, "IR 송신기 초기화 실패");
    }

    // MQTT 클라이언트 초기화 (TLS MQTT 클라이언트 사용으로 인해 주석 처리)
    // g_mqtt_client = new MqttClient();
    // g_mqtt_client->setMessageCallback(onMQTTMessage);

    // ESP32 전용: 전역 인스턴스 설정 (TLS MQTT 클라이언트 사용으로 인해 주석 처리)
    // MqttClient::setGlobalInstance(g_mqtt_client);

    ESP_LOGI(TAG, "하드웨어 초기화 완료");
}

// task 생성
void createTasks() {
    ESP_LOGI(TAG, "task 생성 중");

    // MQTT task 생성
    xTaskCreate(
        mqtt_task,
        "mqtt_task",
        4096,
        NULL,
        5,
        &mqtt_task_handle
    );

    ESP_LOGI(TAG, "task 생성 완료");
}

// Arduino setup 함수
void setup() {
    ESP_LOGI(TAG, "ESP32 IR Remote 시작");
    ESP_LOGI(TAG, "모델: ESP32-WROOM-32E");
    ESP_LOGI(TAG, "Free heap: %d bytes", esp_get_free_heap_size());
    ESP_LOGI(TAG, "Chip revision: %d", esp_chip_info_t().revision);

    // 보안 시스템 초기화 (현재 비활성화 - 컴파일 오류 방지)
    // if (Security::initialize()) {
    //     ESP_LOGI(TAG, "보안 시스템 초기화 성공");
    // } else {
    //     ESP_LOGE(TAG, "보안 시스템 초기화 실패");
    // }
    ESP_LOGI(TAG, "보안 시스템 초기화 건너뜀 (개발 중)");

    // 독립 실행을 위한 설정
    ESP_LOGI(TAG, "독립 실행 모드 활성화");

    // 시리얼 모니터 비활성화 (전력 절약)
    // Serial.end(); // 필요시 주석 해제

    // NVS 초기화
    esp_err_t ret = nvs_flash_init();
    if (ret == ESP_ERR_NVS_NO_FREE_PAGES || ret == ESP_ERR_NVS_NEW_VERSION_FOUND) {
        ESP_ERROR_CHECK(nvs_flash_erase());
        ret = nvs_flash_init();
    }
    ESP_ERROR_CHECK(ret);

    // 설정 로드
    loadConfiguration();

    // WiFi 초기화
    initWiFi();

    // 하드웨어 초기화
    initHardware();

    // task 생성
    createTasks();

    ESP_LOGI(TAG, "ESP32 IR Remote 초기화 완료");
}

// Arduino loop 함수
void loop() {
    // 시리얼 통신 처리 (USB-C 연결)
    if (g_serial_controller) {
        g_serial_controller->loop();
    }

    // 상태 LED 제어 (독립 실행용 - 느린 깜빡임)
    static uint32_t last_led_time = 0;
    static bool led_state = false;

    if (millis() - last_led_time > 2000) { // 2초마다 깜빡임
        led_state = !led_state;
        gpio_set_level(GPIO_NUM_2, led_state ? 1 : 0);
        last_led_time = millis();
    }

    // WiFi 연결 상태 확인 (재연결 시도 제한)
    static uint32_t last_wifi_check = 0;
    static uint32_t wifi_reconnect_count = 0;
    static uint32_t last_reconnect_time = 0;

    if (millis() - last_wifi_check > 10000) { // 10초마다 확인
        wifi_ap_record_t ap_info;
        if (esp_wifi_sta_get_ap_info(&ap_info) != ESP_OK) {
            if (wifi_reconnect_count < 3 && (millis() - last_reconnect_time > 30000)) { // 30초 간격으로 최대 3번
                ESP_LOGW(TAG, "WiFi 연결 끊어짐. 재연결 시도 %d/3...", wifi_reconnect_count + 1);

                // WiFi 재시작 후 연결 시도
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
            wifi_reconnect_count = 0; // 연결 성공 시 카운터 리셋
            last_reconnect_time = 0;
        }
        last_wifi_check = millis();
    }

    // 독립 실행을 위한 딜레이 (전력 절약)
    delay(1000); // 1초 딜레이로 전력 절약
}
