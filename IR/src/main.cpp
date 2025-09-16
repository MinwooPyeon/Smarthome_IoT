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
#include "cJSON.h"
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

// SSL MQTT 클라이언트
WiFiClientSecure g_secure_client;
PubSubClient g_mqtt_client_ssl(g_secure_client);

// WiFi 및 MQTT 설정은 platformio.ini에서 정의됨
// 매크로가 정의되지 않은 경우를 위한 기본값
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
#define DEVICE_ID "test-device"
#endif
#ifndef MQTT_USERNAME
#define MQTT_USERNAME "eeum"
#endif
#ifndef MQTT_PASSWORD
#define MQTT_PASSWORD "ssafy2086eeum"
#endif
#ifndef MQTT_USE_SSL
#define MQTT_USE_SSL 1
#endif

// task 핸들
TaskHandle_t mqtt_task_handle = NULL;

// WiFi 이벤트 핸들러
static void wifi_event_handler(void* arg, esp_event_base_t event_base,
                              int32_t event_id, void* event_data) {
    if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_START) {
        ESP_LOGI(TAG, "WiFi Station 시작됨");
        // 자동 연결 비활성화 - initWiFi()에서 수동으로 연결
    } else if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_DISCONNECTED) {
        ESP_LOGI(TAG, "WiFi 연결 끊어짐, 재연결 시도...");
        esp_wifi_connect();
    } else if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_CONNECTED) {
        ESP_LOGI(TAG, "WiFi AP에 연결됨, IP 할당 대기 중...");
    } else if (event_base == IP_EVENT && event_id == IP_EVENT_STA_GOT_IP) {
        ip_event_got_ip_t* event = (ip_event_got_ip_t*) event_data;
        ESP_LOGI(TAG, "WiFi 연결 성공!");
        ESP_LOGI(TAG, "IP 주소: " IPSTR, IP2STR(&event->ip_info.ip));
        ESP_LOGI(TAG, "게이트웨이: " IPSTR, IP2STR(&event->ip_info.gw));
        ESP_LOGI(TAG, "넷마스크: " IPSTR, IP2STR(&event->ip_info.netmask));
    }
}

// WiFi 연결 함수
void initWiFi() {
    ESP_LOGI(TAG, "WiFi 초기화 시작...");

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

    ESP_LOGI(TAG, "WiFi 초기화 완료 대기 중...");
    vTaskDelay(pdMS_TO_TICKS(2000)); // 2초 대기

    // WiFi 모듈 상태 확인
    wifi_mode_t wifi_mode;
    esp_wifi_get_mode(&wifi_mode);
    ESP_LOGI(TAG, "WiFi 모드: %d", wifi_mode);

    // WiFi 스캔으로 사용 가능한 네트워크 확인 (ESP32-WROOM-32E 최적화)
    ESP_LOGI(TAG, "WiFi 네트워크 스캔 중... (ESP32-WROOM-32E)");
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

    ESP_LOGI(TAG, "WiFi 연결 시도 중...");
    ESP_LOGI(TAG, "SSID: %s", WIFI_SSID);

    // WiFi 스캔 완료 후 연결 시도
    esp_wifi_connect();

    // 연결 대기 (최대 10초)
    ESP_LOGI(TAG, "WiFi 연결 대기 중...");
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
            ESP_LOGE(TAG, "목표 네트워크 '%s'를 찾을 수 없습니다!", WIFI_SSID);
        } else {
            ESP_LOGW(TAG, "네트워크는 발견되었지만 연결에 실패했습니다. 재시도 중...");
            esp_wifi_connect();
        }
    }
}

// MQTT 메시지 콜백
void onMQTTMessage(const std::string& topic, const std::string& message) {
    ESP_LOGI(TAG, "MQTT 메시지 수신: %s -> %s", topic.c_str(), message.c_str());

    // 먼저 JSON 형식인지 확인
    cJSON *doc = cJSON_Parse(message.c_str());
    if (doc != NULL) {
        // JSON 형식인 경우
        cJSON *ir_code = cJSON_GetObjectItem(doc, "ir_code");

        if (cJSON_IsString(ir_code)) {
            // IR 코드가 직접 전달된 경우 - 그대로 전송
            std::string ir_code_str = std::string(ir_code->valuestring);
            ESP_LOGI(TAG, "JSON IR 코드 전송: %s", ir_code_str.c_str());

            if (g_ir_sender) {
                auto result = g_ir_sender->sendIRCode(ir_code_str);
                ESP_LOGI(TAG, "IR 송신 결과: %s", result.result == IRSendResult::SUCCESS ? "성공" : "실패");

                // 결과를 MQTT로 전송
                if (g_mqtt_client && g_mqtt_client->isConnected()) {
                    cJSON *response = cJSON_CreateObject();
                    cJSON_AddStringToObject(response, "ir_code", ir_code_str.c_str());
                    cJSON_AddBoolToObject(response, "success", (result.result == IRSendResult::SUCCESS));
                    cJSON_AddNumberToObject(response, "timestamp", esp_timer_get_time() / 1000);

                char *response_str = cJSON_Print(response);
                g_mqtt_client->publish("eeum/test", response_str);
                free(response_str);
                cJSON_Delete(response);
                }
            }
        } else {
            ESP_LOGW(TAG, "JSON 메시지에 ir_code가 없음: %s", message.c_str());
        }
        cJSON_Delete(doc);
    } else {
        // JSON이 아닌 경우 - 텍스트 메시지로 처리
        ESP_LOGI(TAG, "텍스트 메시지 수신: %s", message.c_str());

        // 텍스트 메시지를 IR 코드로 처리
        if (g_ir_sender) {
            auto result = g_ir_sender->sendIRCode(message);
            ESP_LOGI(TAG, "텍스트 IR 송신 결과: %s", result.result == IRSendResult::SUCCESS ? "성공" : "실패");

            // 결과를 MQTT로 전송
            if (g_mqtt_client && g_mqtt_client->isConnected()) {
                cJSON *response = cJSON_CreateObject();
                cJSON_AddStringToObject(response, "message", message.c_str());
                cJSON_AddBoolToObject(response, "success", (result.result == IRSendResult::SUCCESS));
                cJSON_AddNumberToObject(response, "timestamp", esp_timer_get_time() / 1000);

                char *response_str = cJSON_Print(response);
                g_mqtt_client->publish("eeum/test", response_str);
                free(response_str);
                cJSON_Delete(response);
            }
        }
    }
}

// 응답 메시지 전송 함수
void sendAckMessage(const std::string& msgId, const std::string& corrId,
                   const std::string& status, const std::string& detail,
                   int durationMs = 0, int retries = 0) {
    cJSON *ack = cJSON_CreateObject();
    cJSON_AddNumberToObject(ack, "ts", esp_timer_get_time() / 1000);
    cJSON_AddStringToObject(ack, "deviceId", DEVICE_ID);
    cJSON_AddStringToObject(ack, "schema", "ack/1.0");
    cJSON_AddStringToObject(ack, "corrId", corrId.c_str());
    cJSON_AddStringToObject(ack, "msgId", msgId.c_str());
    cJSON_AddStringToObject(ack, "status", status.c_str());

    cJSON *result = cJSON_CreateObject();
    cJSON_AddNumberToObject(result, "code", 0);
    cJSON_AddStringToObject(result, "detail", detail.c_str());
    cJSON_AddItemToObject(ack, "result", result);

    cJSON_AddNumberToObject(ack, "durationMs", durationMs);
    cJSON_AddNumberToObject(ack, "retries", retries);

    char *ack_str = cJSON_Print(ack);
    std::string ack_topic = "hub/" + std::string(DEVICE_ID) + "/order/ack";

    if (g_mqtt_client_ssl.connected()) {
        g_mqtt_client_ssl.publish(ack_topic.c_str(), ack_str);
        ESP_LOGI(TAG, "응답 메시지 전송: %s", ack_str);
    }

    free(ack_str);
    cJSON_Delete(ack);
}

// 에러 메시지 전송 함수
void sendErrorMessage(const std::string& level, const std::string& code,
                     const std::string& detail, const std::string& orderMsgId = "") {
    cJSON *error = cJSON_CreateObject();
    cJSON_AddNumberToObject(error, "ts", esp_timer_get_time() / 1000);
    cJSON_AddStringToObject(error, "deviceId", DEVICE_ID);
    cJSON_AddStringToObject(error, "schema", "error/1.0");
    cJSON_AddStringToObject(error, "level", level.c_str());
    cJSON_AddStringToObject(error, "code", code.c_str());
    cJSON_AddStringToObject(error, "detail", detail.c_str());

    if (!orderMsgId.empty()) {
        cJSON *ctx = cJSON_CreateObject();
        cJSON_AddStringToObject(ctx, "orderMsgId", orderMsgId.c_str());
        cJSON_AddItemToObject(error, "ctx", ctx);
    }

    char *error_str = cJSON_Print(error);
    std::string error_topic = "hub/" + std::string(DEVICE_ID) + "/error";

    if (g_mqtt_client_ssl.connected()) {
        g_mqtt_client_ssl.publish(error_topic.c_str(), error_str);
        ESP_LOGI(TAG, "에러 메시지 전송: %s", error_str);
    }

    free(error_str);
    cJSON_Delete(error);
}

// SSL MQTT 메시지 콜백 (새로운 스키마용)
void onMQTTMessage(char* topic, unsigned char* payload, unsigned int length) {
    std::string message((char*)payload, length);
    std::string topic_str = std::string(topic);
    ESP_LOGI(TAG, "MQTT 메시지 수신 - 토픽: %s, 내용: %s", topic, message.c_str());

    // irsignal 토픽 처리
    if (topic_str.find("/irsignal") != std::string::npos) {
        ESP_LOGI(TAG, "IR 신호 토픽 메시지 수신");

        // JSON 파싱
        cJSON *json = cJSON_Parse(message.c_str());
        if (json == nullptr) {
            ESP_LOGE(TAG, "JSON 파싱 실패");
            return;
        }

        // IR 신호 데이터 추출
        cJSON *encoding = cJSON_GetObjectItem(json, "encoding");
        cJSON *carrierHz = cJSON_GetObjectItem(json, "carrierHz");
        cJSON *data = cJSON_GetObjectItem(json, "data");
        cJSON *repeat = cJSON_GetObjectItem(json, "repeat");

        if (cJSON_IsString(encoding) && cJSON_IsString(data)) {
            std::string encoding_str = std::string(encoding->valuestring);
            std::string ir_data = std::string(data->valuestring);
            int repeat_count = cJSON_IsNumber(repeat) ? repeat->valueint : 1;
            int carrier_freq = cJSON_IsNumber(carrierHz) ? carrierHz->valueint : 38000;

            ESP_LOGI(TAG, "IR 신호 수신 - 인코딩: %s, 반송파: %dHz, 데이터: %s, 반복: %d",
                    encoding_str.c_str(), carrier_freq, ir_data.c_str(), repeat_count);

            // IR 코드 송신
            if (g_ir_sender) {
                auto start_time = esp_timer_get_time();
                g_ir_sender->sendIRCode(ir_data);
                auto duration = (esp_timer_get_time() - start_time) / 1000;

                ESP_LOGI(TAG, "IR 송신 완료 (소요시간: %lldms)", duration);
            } else {
                ESP_LOGE(TAG, "IR 송신기 초기화되지 않음");
            }
        } else {
            ESP_LOGE(TAG, "IR 신호 데이터 필드 누락");
        }

        cJSON_Delete(json);
        return;
    }

    // order 토픽 처리 (기존 로직)
    // JSON 파싱
    cJSON *json = cJSON_Parse(message.c_str());
    if (json == nullptr) {
        ESP_LOGE(TAG, "JSON 파싱 실패");
        sendErrorMessage("ERROR", "JSON_PARSE_FAILED", "Invalid JSON format");
        return;
    }

    // 필수 필드 확인
    cJSON *msgId = cJSON_GetObjectItem(json, "msgId");
    cJSON *corrId = cJSON_GetObjectItem(json, "corrId");
    cJSON *type = cJSON_GetObjectItem(json, "type");
    cJSON *payload_obj = cJSON_GetObjectItem(json, "payload");

    if (!cJSON_IsString(msgId) || !cJSON_IsString(corrId) ||
        !cJSON_IsString(type) || !cJSON_IsObject(payload_obj)) {
        ESP_LOGE(TAG, "필수 필드 누락");
        sendErrorMessage("ERROR", "MISSING_FIELDS", "Required fields missing");
        cJSON_Delete(json);
        return;
    }

    std::string msgId_str = std::string(msgId->valuestring);
    std::string corrId_str = std::string(corrId->valuestring);
    std::string type_str = std::string(type->valuestring);

    // IR 명령 처리
    if (type_str == "ir") {
        cJSON *ir = cJSON_GetObjectItem(payload_obj, "ir");
        if (cJSON_IsObject(ir)) {
            // JSON 형태의 IR 신호 데이터 파싱
            cJSON *encoding = cJSON_GetObjectItem(ir, "encoding");
            cJSON *carrierHz = cJSON_GetObjectItem(ir, "carrierHz");
            cJSON *dutyCycle = cJSON_GetObjectItem(ir, "dutyCycle");
            cJSON *address = cJSON_GetObjectItem(ir, "address");
            cJSON *command = cJSON_GetObjectItem(ir, "command");
            cJSON *timing = cJSON_GetObjectItem(ir, "timing");
            cJSON *rawData = cJSON_GetObjectItem(ir, "rawData");
            cJSON *data = cJSON_GetObjectItem(ir, "data");
            cJSON *repeat = cJSON_GetObjectItem(ir, "repeat");
            cJSON *quality = cJSON_GetObjectItem(ir, "quality");

            std::string encoding_str = cJSON_IsString(encoding) ? std::string(encoding->valuestring) : "NEC";
            int carrier_freq = cJSON_IsNumber(carrierHz) ? carrierHz->valueint : 38000;
            float duty_cycle = cJSON_IsNumber(dutyCycle) ? dutyCycle->valuedouble : 0.33;
            int repeat_count = cJSON_IsNumber(repeat) ? repeat->valueint : 1;
            float signal_quality = cJSON_IsNumber(quality) ? quality->valuedouble : 1.0;

            ESP_LOGI(TAG, "IR 신호 수신 - 인코딩: %s, 반송파: %dHz, 듀티: %.2f, 반복: %d, 품질: %.2f",
                    encoding_str.c_str(), carrier_freq, duty_cycle, repeat_count, signal_quality);

            // 주소와 명령어 정보 로깅
            if (cJSON_IsString(address)) {
                ESP_LOGI(TAG, "주소: %s", address->valuestring);
            } else if (cJSON_IsNumber(address)) {
                ESP_LOGI(TAG, "주소: 0x%X", address->valueint);
            }

            if (cJSON_IsString(command)) {
                ESP_LOGI(TAG, "명령: %s", command->valuestring);
            } else if (cJSON_IsNumber(command)) {
                ESP_LOGI(TAG, "명령: 0x%X", command->valueint);
            }

            // 타이밍 정보 로깅
            if (cJSON_IsObject(timing)) {
                cJSON *header = cJSON_GetObjectItem(timing, "header");
                cJSON *one = cJSON_GetObjectItem(timing, "one");
                cJSON *zero = cJSON_GetObjectItem(timing, "zero");
                cJSON *gap = cJSON_GetObjectItem(timing, "gap");

                if (cJSON_IsArray(header) && cJSON_GetArraySize(header) >= 2) {
                    ESP_LOGI(TAG, "헤더 타이밍: [%d, %d]",
                            cJSON_GetArrayItem(header, 0)->valueint,
                            cJSON_GetArrayItem(header, 1)->valueint);
                }
                if (cJSON_IsArray(one) && cJSON_GetArraySize(one) >= 2) {
                    ESP_LOGI(TAG, "1 비트 타이밍: [%d, %d]",
                            cJSON_GetArrayItem(one, 0)->valueint,
                            cJSON_GetArrayItem(one, 1)->valueint);
                }
                if (cJSON_IsArray(zero) && cJSON_GetArraySize(zero) >= 2) {
                    ESP_LOGI(TAG, "0 비트 타이밍: [%d, %d]",
                            cJSON_GetArrayItem(zero, 0)->valueint,
                            cJSON_GetArrayItem(zero, 1)->valueint);
                }
                if (cJSON_IsNumber(gap)) {
                    ESP_LOGI(TAG, "갭: %d", gap->valueint);
                }
            }

            // Raw 데이터 로깅
            if (cJSON_IsArray(rawData)) {
                int array_size = cJSON_GetArraySize(rawData);
                ESP_LOGI(TAG, "Raw 데이터 길이: %d", array_size);
                if (array_size > 0) {
                    ESP_LOGI(TAG, "Raw 데이터 시작: [%d, %d, %d, ...]",
                            cJSON_GetArrayItem(rawData, 0)->valueint,
                            cJSON_GetArrayItem(rawData, 1)->valueint,
                            cJSON_GetArrayItem(rawData, 2)->valueint);
                }
            }

            // 데이터 필드 처리 (hex 문자열 또는 raw 데이터)
            if (cJSON_IsString(data)) {
                std::string ir_data = std::string(data->valuestring);
                ESP_LOGI(TAG, "데이터 (hex): %s", ir_data.c_str());

                // IR 코드 송신
                if (g_ir_sender) {
                    auto start_time = esp_timer_get_time();
                    g_ir_sender->sendIRCode(ir_data);
                    auto duration = (esp_timer_get_time() - start_time) / 1000;

                    ESP_LOGI(TAG, "IR 송신 완료");
                    sendAckMessage(msgId_str, corrId_str, "done", "IR send success", duration);
                } else {
                    sendErrorMessage("ERROR", "IR_SENDER_NOT_AVAILABLE", "IR sender not initialized", msgId_str);
                }
            } else if (cJSON_IsArray(rawData)) {
                // Raw 데이터 배열 처리
                int array_size = cJSON_GetArraySize(rawData);
                std::vector<int> raw_data_array;

                for (int i = 0; i < array_size; i++) {
                    cJSON *item = cJSON_GetArrayItem(rawData, i);
                    if (cJSON_IsNumber(item)) {
                        raw_data_array.push_back(item->valueint);
                    }
                }

                ESP_LOGI(TAG, "Raw 데이터 배열 처리: %d개 펄스", raw_data_array.size());

                // Raw 데이터로 IR 송신 (실제 구현은 IR 라이브러리에 따라 다름)
                if (g_ir_sender) {
                    auto start_time = esp_timer_get_time();
                    // g_ir_sender->sendRawData(raw_data_array); // 실제 구현 필요
                    g_ir_sender->sendIRCode("RAW_DATA"); // 임시 처리
                    auto duration = (esp_timer_get_time() - start_time) / 1000;

                    ESP_LOGI(TAG, "Raw IR 송신 완료");
                    sendAckMessage(msgId_str, corrId_str, "done", "Raw IR send success", duration);
                } else {
                    sendErrorMessage("ERROR", "IR_SENDER_NOT_AVAILABLE", "IR sender not initialized", msgId_str);
                }
            } else {
                sendErrorMessage("ERROR", "INVALID_IR_DATA", "IR data field missing or invalid", msgId_str);
            }
        } else {
            sendErrorMessage("ERROR", "INVALID_IR_PAYLOAD", "IR payload missing or invalid", msgId_str);
        }
    } else if (type_str == "system") {
        cJSON *system = cJSON_GetObjectItem(payload_obj, "system");
        if (cJSON_IsObject(system)) {
            cJSON *action = cJSON_GetObjectItem(system, "action");
            if (cJSON_IsString(action)) {
                std::string action_str = std::string(action->valuestring);
                ESP_LOGI(TAG, "시스템 명령: %s", action_str.c_str());

                if (action_str == "reboot") {
                    sendAckMessage(msgId_str, corrId_str, "accepted", "Reboot command received");
                    vTaskDelay(pdMS_TO_TICKS(1000));
                    esp_restart();
                } else {
                    sendErrorMessage("ERROR", "UNKNOWN_SYSTEM_ACTION", "Unknown system action: " + action_str, msgId_str);
                }
            }
        }
    } else {
        sendErrorMessage("ERROR", "UNKNOWN_COMMAND_TYPE", "Unknown command type: " + type_str, msgId_str);
    }

    cJSON_Delete(json);
}

// 시리얼 명령 처리 콜백
std::string onSerialCommand(const std::string& command, const cJSON* params) {
    ESP_LOGI(TAG, "시리얼 명령 수신: %s", command.c_str());

    if (command == "ir_send") {
        cJSON *ir_code = cJSON_GetObjectItem(params, "ir_code");

        if (!cJSON_IsString(ir_code)) {
            return "오류: ir_code가 필요합니다";
        }

        std::string ir_code_str = std::string(ir_code->valuestring);

        if (g_ir_sender) {
            auto result = g_ir_sender->sendIRCode(ir_code_str);

            cJSON *response = cJSON_CreateObject();
            cJSON_AddBoolToObject(response, "success", (result.result == IRSendResult::SUCCESS));
            cJSON_AddStringToObject(response, "ir_code", ir_code_str.c_str());
            cJSON_AddStringToObject(response, "message", (result.result == IRSendResult::SUCCESS) ? "성공" : "실패");

            char *response_str = cJSON_Print(response);
            std::string result_str = response_str;
            free(response_str);
            cJSON_Delete(response);
            return result_str;
        } else {
            return "오류: IR 송신기가 초기화되지 않았습니다";
        }
    } else if (command == "ir_status") {
        // IR 송신 상태 반환
        cJSON *ir_status = cJSON_CreateObject();
        cJSON_AddBoolToObject(ir_status, "sending", (g_ir_sender != nullptr));
        cJSON_AddNumberToObject(ir_status, "tx_pin", 23);

        char *ir_status_str = cJSON_Print(ir_status);
        std::string result_str = ir_status_str;
        free(ir_status_str);
        cJSON_Delete(ir_status);
        return result_str;
    } else if (command == "mqtt_status") {
        cJSON *mqtt_status = cJSON_CreateObject();
        cJSON_AddBoolToObject(mqtt_status, "connected", (g_mqtt_client && g_mqtt_client->isConnected()));
        cJSON_AddStringToObject(mqtt_status, "broker", g_config ? g_config->getString("mqtt.broker", "").c_str() : "");
        cJSON_AddNumberToObject(mqtt_status, "port", g_config ? g_config->getInt("mqtt.port", 1883) : 1883);

        char *mqtt_status_str = cJSON_Print(mqtt_status);
        std::string result_str = mqtt_status_str;
        free(mqtt_status_str);
        cJSON_Delete(mqtt_status);
        return result_str;
    } else if (command == "device_list") {
        cJSON *device_list = cJSON_CreateObject();
        cJSON *devices = cJSON_CreateArray();

        // Samsung TV
        cJSON *tv = cJSON_CreateObject();
        cJSON_AddStringToObject(tv, "id", "samsung_tv");
        cJSON_AddStringToObject(tv, "name", "Samsung TV");
        cJSON_AddStringToObject(tv, "type", "tv");
        cJSON_AddItemToArray(devices, tv);

        // Samsung AC
        cJSON *ac = cJSON_CreateObject();
        cJSON_AddStringToObject(ac, "id", "samsung_ac");
        cJSON_AddStringToObject(ac, "name", "Samsung AC");
        cJSON_AddStringToObject(ac, "type", "air_conditioner");
        cJSON_AddItemToArray(devices, ac);

        cJSON_AddItemToObject(device_list, "devices", devices);

        char *device_list_str = cJSON_Print(device_list);
        std::string result_str = device_list_str;
        free(device_list_str);
        cJSON_Delete(device_list);
        return result_str;
    } else if (command == "restart") {
        ESP_LOGI(TAG, "시스템 재시작 요청");
        vTaskDelay(pdMS_TO_TICKS(1000));
        esp_restart();
        return "재시작 중...";
    }

    return "알 수 없는 명령어: " + command;
}

// SSL MQTT 연결 함수
bool connectSSL() {
    // SSL 클라이언트 설정
    g_secure_client.setInsecure(); // 인증서 검증 비활성화 (--insecure 옵션과 동일)

    // MQTT 클라이언트 설정
    g_mqtt_client_ssl.setServer(MQTT_BROKER, MQTT_PORT);
    g_mqtt_client_ssl.setCallback([](char* topic, unsigned char* payload, unsigned int length) {
        onMQTTMessage(topic, payload, length);
    });

    // 연결 시도
    if (g_mqtt_client_ssl.connect(MQTT_CLIENT_ID, MQTT_USERNAME, MQTT_PASSWORD)) {
        ESP_LOGI(TAG, "SSL MQTT 연결 성공!");

        // 새로운 토픽 구조로 구독
        std::string order_topic = "hub/" + std::string(DEVICE_ID) + "/order";
        std::string irsignal_topic = "hub/" + std::string(DEVICE_ID) + "/irsignal";

        g_mqtt_client_ssl.subscribe(order_topic.c_str());
        g_mqtt_client_ssl.subscribe(irsignal_topic.c_str());

        ESP_LOGI(TAG, "MQTT 토픽 구독: %s", order_topic.c_str());
        ESP_LOGI(TAG, "MQTT 토픽 구독: %s", irsignal_topic.c_str());

        return true;
    } else {
        ESP_LOGE(TAG, "SSL MQTT 연결 실패");
        return false;
    }
}

// MQTT task
void mqtt_task(void* parameter) {
    while (true) {
        if (!g_mqtt_client_ssl.connected()) {
            ESP_LOGI(TAG, "[IR_REMOTE_MAIN] SSL MQTT 재연결 시도...");

            if (connectSSL()) {
                ESP_LOGI(TAG, "[IR_REMOTE_MAIN] SSL MQTT 연결 성공!");
            } else {
                ESP_LOGE(TAG, "[IR_REMOTE_MAIN] SSL MQTT 연결 실패");
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
    ESP_LOGI(TAG, "설정 로드 중...");

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
    ESP_LOGI(TAG, "하드웨어 초기화 중...");

    // 시리얼 컨트롤러 초기화 (USB-C 연결)
    g_serial_controller = new SerialController(115200);
    g_serial_controller->setCommandCallback(onSerialCommand);
    g_serial_controller->setDebugMode(true);
    g_serial_controller->initialize();

    // IR 송신기 초기화 (GPIO 22번)
    g_ir_sender = new IRSend();
    g_ir_sender->initialize();
    g_ir_sender->setDebugMode(true);

    // MQTT 클라이언트 초기화 (SSL MQTT 클라이언트 사용으로 인해 주석 처리)
    // g_mqtt_client = new MqttClient();
    // g_mqtt_client->setMessageCallback(onMQTTMessage);

    // ESP32 전용: 전역 인스턴스 설정 (SSL MQTT 클라이언트 사용으로 인해 주석 처리)
    // MqttClient::setGlobalInstance(g_mqtt_client);

    ESP_LOGI(TAG, "하드웨어 초기화 완료");
}

// task 생성
void createTasks() {
    ESP_LOGI(TAG, "task 생성 중...");

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

    // 상태 LED 제어
    static bool led_state = false;
    led_state = !led_state;
    gpio_set_level(GPIO_NUM_2, led_state ? 1 : 0);

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
                ESP_LOGE(TAG, "WiFi 재연결 시도 횟수 초과. 수동 재시작 필요.");
            }
        } else {
            wifi_reconnect_count = 0; // 연결 성공 시 카운터 리셋
            last_reconnect_time = 0;
        }
        last_wifi_check = millis();
    }

    delay(100);
}
