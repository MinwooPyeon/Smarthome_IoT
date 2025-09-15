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

// WiFi 설정 - 실제 네트워크 정보로 변경하세요
#define WIFI_SSID "iPhone"
#define WIFI_PASSWORD "49555412"

// MQTT 설정 - 실제 MQTT 브로커 주소로 변경하세요
#define MQTT_BROKER "실제_MQTT_브로커_주소"
#define MQTT_PORT 1883
#define MQTT_CLIENT_ID "esp32_ir_controller"

// 태스크 핸들
TaskHandle_t mqtt_task_handle = NULL;

// WiFi 이벤트 핸들러
static void wifi_event_handler(void* arg, esp_event_base_t event_base,
                              int32_t event_id, void* event_data) {
    if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_START) {
        esp_wifi_connect();
    } else if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_DISCONNECTED) {
        ESP_LOGI(TAG, "WiFi 연결 끊어짐, 재연결 시도...");
        esp_wifi_connect();
    } else if (event_base == IP_EVENT && event_id == IP_EVENT_STA_GOT_IP) {
        ip_event_got_ip_t* event = (ip_event_got_ip_t*) event_data;
        ESP_LOGI(TAG, "WiFi 연결 성공!");
        ESP_LOGI(TAG, "IP 주소: " IPSTR, IP2STR(&event->ip_info.ip));
    }
}

// WiFi 연결 함수
void initWiFi() {
    esp_netif_init();
    esp_event_loop_create_default();
    esp_netif_create_default_wifi_sta();

    wifi_init_config_t cfg = WIFI_INIT_CONFIG_DEFAULT();
    esp_wifi_init(&cfg);

    // 이벤트 핸들러 등록
    esp_event_handler_register(WIFI_EVENT, ESP_EVENT_ANY_ID, &wifi_event_handler, NULL);
    esp_event_handler_register(IP_EVENT, IP_EVENT_STA_GOT_IP, &wifi_event_handler, NULL);

    wifi_config_t wifi_config = {};
    strcpy((char*)wifi_config.sta.ssid, WIFI_SSID);
    strcpy((char*)wifi_config.sta.password, WIFI_PASSWORD);
    wifi_config.sta.threshold.authmode = WIFI_AUTH_WPA2_PSK;

    esp_wifi_set_mode(WIFI_MODE_STA);
    esp_wifi_set_config(WIFI_IF_STA, &wifi_config);
    esp_wifi_start();

    ESP_LOGI(TAG, "WiFi 연결 시도 중...");
    ESP_LOGI(TAG, "SSID: %s", WIFI_SSID);

    // WiFi 스캔으로 사용 가능한 네트워크 확인
    ESP_LOGI(TAG, "WiFi 네트워크 스캔 중...");
    wifi_scan_config_t scan_config = {};
    scan_config.ssid = NULL;
    scan_config.bssid = NULL;
    scan_config.channel = 0;
    scan_config.show_hidden = true;
    scan_config.scan_type = WIFI_SCAN_TYPE_ACTIVE;
    scan_config.scan_time.active.min = 100;
    scan_config.scan_time.active.max = 300;

    esp_wifi_scan_start(&scan_config, true);

    uint16_t ap_count = 0;
    esp_wifi_scan_get_ap_num(&ap_count);
    ESP_LOGI(TAG, "발견된 WiFi 네트워크: %d개", ap_count);

    if (ap_count > 0) {
        wifi_ap_record_t ap_records[ap_count];
        esp_wifi_scan_get_ap_records(&ap_count, ap_records);

        for (int i = 0; i < ap_count; i++) {
            ESP_LOGI(TAG, "WiFi: %s (RSSI: %d)", ap_records[i].ssid, ap_records[i].rssi);
        }
    }
}

// MQTT 메시지 콜백
void onMQTTMessage(const std::string& topic, const std::string& message) {
    ESP_LOGI(TAG, "MQTT 메시지 수신: %s -> %s", topic.c_str(), message.c_str());

    // cJSON 사용
    cJSON *doc = cJSON_Parse(message.c_str());
    if (doc == NULL) {
        ESP_LOGE(TAG, "JSON 파싱 오류");
        return;
    }

    // MQTT 메시지에서 IR 코드 직접 추출
    cJSON *ir_code = cJSON_GetObjectItem(doc, "ir_code");

    if (cJSON_IsString(ir_code)) {
        // IR 코드가 직접 전달된 경우 - 그대로 전송
        std::string ir_code_str = std::string(ir_code->valuestring);
        ESP_LOGI(TAG, "IR 코드 전송: %s", ir_code_str.c_str());

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
                g_mqtt_client->publish("irremote/response", response_str);
                free(response_str);
                cJSON_Delete(response);
            }
        }
    } else {
        ESP_LOGW(TAG, "IR 코드가 포함되지 않은 메시지: %s", message.c_str());
    }

    cJSON_Delete(doc);
}

// IR 코드 수신 콜백 (송신만 필요하므로 제거)

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
        cJSON_AddNumberToObject(ir_status, "tx_pin", 22);

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

// MQTT 태스크
void mqtt_task(void* parameter) {
    while (true) {
        if (g_mqtt_client) {
            if (!g_mqtt_client->isConnected()) {
                ESP_LOGI(TAG, "MQTT 재연결 시도...");
                if (g_mqtt_client->connect(MQTT_BROKER, MQTT_PORT)) {
                    ESP_LOGI(TAG, "MQTT 연결 성공!");
                    g_mqtt_client->subscribe("irremote/control");
                } else {
                    ESP_LOGE(TAG, "MQTT 연결 실패!");
                }
            } else {
                g_mqtt_client->loop();
            }
        }
        vTaskDelay(pdMS_TO_TICKS(100));
    }
}

// IR 수신 태스크 (송신만 필요하므로 제거)

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

    // MQTT 클라이언트 초기화
    g_mqtt_client = new MqttClient();
    g_mqtt_client->setMessageCallback(onMQTTMessage);

    // ESP32 전용: 전역 인스턴스 설정
    MqttClient::setGlobalInstance(g_mqtt_client);

    ESP_LOGI(TAG, "하드웨어 초기화 완료");
}

// 태스크 생성
void createTasks() {
    ESP_LOGI(TAG, "태스크 생성 중...");

    // MQTT 태스크 생성
    xTaskCreate(
        mqtt_task,
        "mqtt_task",
        4096,
        NULL,
        5,
        &mqtt_task_handle
    );

    ESP_LOGI(TAG, "태스크 생성 완료");
}

// Arduino setup 함수
void setup() {
    ESP_LOGI(TAG, "ESP32 IR Remote 시작");
    ESP_LOGI(TAG, "Free heap: %d bytes", esp_get_free_heap_size());

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

    // 태스크 생성
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

    // WiFi 연결 상태 확인
    wifi_ap_record_t ap_info;
    if (esp_wifi_sta_get_ap_info(&ap_info) != ESP_OK) {
        ESP_LOGW(TAG, "WiFi 연결 끊어짐. 재연결 시도...");
        initWiFi();
    }

    delay(100);
}
