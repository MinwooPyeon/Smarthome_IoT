#include <Arduino.h>
#include "WiFi.h"
#include "nvs_flash.h"
#include "esp_log.h"
#include "esp_system.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/queue.h"
#include "ArduinoJson.h"

// IR Remote 컴포넌트 포함
#include "config.h"
#include "mqtt_client.h"
#include "ir_receiver.h"
#include "appliance_controller.h"
#include "irsend.h"

static const char* TAG = "IR_REMOTE_MAIN";

// 전역 변수 (ESP32에서는 포인터 사용)
Config* g_config = nullptr;
MqttClient* g_mqtt_client = nullptr;
IRReceiver* g_ir_receiver = nullptr;
ApplianceController* g_appliance_controller = nullptr;
IRSend* g_ir_sender = nullptr;

// WiFi 설정
const char* WIFI_SSID = "your_wifi_ssid";
const char* WIFI_PASSWORD = "your_wifi_password";

// MQTT 설정
const char* MQTT_BROKER = "192.168.1.100";
const int MQTT_PORT = 1883;
const char* MQTT_CLIENT_ID = "esp32_ir_controller";

// 태스크 핸들
TaskHandle_t mqtt_task_handle = NULL;
TaskHandle_t ir_task_handle = NULL;

// WiFi 연결 함수
void initWiFi() {
    WiFi.mode(WIFI_STA);
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    
    ESP_LOGI(TAG, "WiFi 연결 시도 중...");
    
    int retry_count = 0;
    while (WiFi.status() != WL_CONNECTED && retry_count < 20) {
        vTaskDelay(pdMS_TO_TICKS(500));
        ESP_LOGI(TAG, "WiFi 연결 시도 %d/20", retry_count + 1);
        retry_count++;
    }
    
    if (WiFi.status() == WL_CONNECTED) {
        ESP_LOGI(TAG, "WiFi 연결 성공!");
        ESP_LOGI(TAG, "IP 주소: %s", WiFi.localIP().toString().c_str());
    } else {
        ESP_LOGE(TAG, "WiFi 연결 실패!");
    }
}

// MQTT 메시지 콜백
void onMQTTMessage(const std::string& topic, const std::string& message) {
    ESP_LOGI(TAG, "MQTT 메시지 수신: %s -> %s", topic.c_str(), message.c_str());
    
    // ArduinoJson 사용
    DynamicJsonDocument doc(1024);
    DeserializationError error = deserializeJson(doc, message);
    
    if (error) {
        ESP_LOGE(TAG, "JSON 파싱 오류: %s", error.c_str());
        return;
    }
    
    if (doc.containsKey("command") && doc.containsKey("device_id")) {
        String command = doc["command"];
        String device_id = doc["device_id"];
        
        // IR 송신기로 직접 제어
        if (g_ir_sender) {
            String control_signal = device_id + "_" + command;
            auto result = g_ir_sender->sendControlSignal(control_signal.c_str());
            
            ESP_LOGI(TAG, "IR 송신 결과: %s", result.result == IRSendResult::SUCCESS ? "성공" : "실패");
            
            // 결과를 MQTT로 전송
            if (g_mqtt_client && g_mqtt_client->isConnected()) {
                DynamicJsonDocument response(256);
                response["device_id"] = device_id;
                response["command"] = command;
                response["success"] = (result.result == IRSendResult::SUCCESS);
                response["timestamp"] = esp_timer_get_time() / 1000;
                
                String response_str;
                serializeJson(response, response_str);
                g_mqtt_client->publish("irremote/response", response_str.c_str());
            }
        }
    }
}

// IR 코드 수신 콜백
void onIRCodeReceived(const std::string& ir_code) {
    ESP_LOGI(TAG, "IR 코드 수신: %s", ir_code.c_str());
    
    // MQTT로 IR 코드 전송
    if (g_mqtt_client && g_mqtt_client->isConnected()) {
        DynamicJsonDocument ir_msg(256);
        ir_msg["ir_code"] = ir_code.c_str();
        ir_msg["timestamp"] = esp_timer_get_time() / 1000; // ms
        ir_msg["device_id"] = "esp32_ir_01";
        
        String ir_msg_str;
        serializeJson(ir_msg, ir_msg_str);
        g_mqtt_client->publish("irremote/received", ir_msg_str.c_str());
    }
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

// IR 수신 태스크
void ir_task(void* parameter) {
    while (true) {
        if (g_ir_receiver) {
            std::string ir_code = g_ir_receiver->receiveIRCode();
            if (!ir_code.empty()) {
                onIRCodeReceived(ir_code);
            }
        }
        vTaskDelay(pdMS_TO_TICKS(10));
    }
}

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
    
    // IR 수신기 초기화
    int ir_rx_pin = g_config->getInt("ir.rx_pin", 5);
    g_ir_receiver = new IRReceiver(ir_rx_pin);
    g_ir_receiver->setIRCodeCallback(onIRCodeReceived);
    g_ir_receiver->startReceiving();
    
    // IR 송신기 초기화
    g_ir_sender = new IRSend();
    g_ir_sender->initialize();
    g_ir_sender->setDebugMode(true);
    
    // 가전기기 제어기 초기화
    g_appliance_controller = new ApplianceController();
    
    // MQTT 클라이언트 초기화
    g_mqtt_client = new MqttClient();
    g_mqtt_client->setMessageCallback(onMQTTMessage);
    
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
    
    // IR 수신 태스크 생성
    xTaskCreate(
        ir_task,
        "ir_task",
        4096,
        NULL,
        6,
        &ir_task_handle
    );
    
    ESP_LOGI(TAG, "태스크 생성 완료");
}

// 메인 함수
extern "C" void app_main() {
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
    
    // 메인 루프
    while (true) {
        // 상태 LED 제어
        static bool led_state = false;
        led_state = !led_state;
        digitalWrite(2, led_state ? HIGH : LOW);
        
        // WiFi 연결 상태 확인
        if (WiFi.status() != WL_CONNECTED) {
            ESP_LOGW(TAG, "WiFi 연결 끊어짐. 재연결 시도...");
            initWiFi();
        }
        
        vTaskDelay(pdMS_TO_TICKS(1000));
    }
}
