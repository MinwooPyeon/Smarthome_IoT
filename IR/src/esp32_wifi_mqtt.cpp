#include "esp32_wifi_mqtt.h"
#include <esp_log.h>
#include <esp_wifi.h>
#include <esp_event.h>
#include <esp_netif.h>
#include <mqtt_client.h>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include <freertos/event_groups.h>
#include <algorithm>

static const char* TAG = "ESP32_WIFI_MQTT";

// 정적 멤버 변수 정의
const char* ESP32WiFiMQTT::TAG = "ESP32_WIFI_MQTT";
const int ESP32WiFiMQTT::WIFI_CONNECTED_BIT = BIT0;
const int ESP32WiFiMQTT::WIFI_FAIL_BIT = BIT1;
const int ESP32WiFiMQTT::MQTT_CONNECTED_BIT = BIT2;
const int ESP32WiFiMQTT::MQTT_FAIL_BIT = BIT3;

ESP32WiFiMQTT::ESP32WiFiMQTT()
    : wifi_connected_(false), mqtt_connected_(false), initialized_(false),
      netif_(nullptr), mqtt_client_(nullptr), wifi_event_group_(nullptr), mqtt_event_group_(nullptr) {
    
    // 통계 초기화
    stats_.wifi_connected = false;
    stats_.mqtt_connected = false;
    stats_.mqtt_messages_sent = 0;
    stats_.mqtt_messages_received = 0;
    stats_.last_mqtt_activity = 0;
    
    // 뮤텍스 초기화
    portMUX_INITIALIZER(&stats_mutex_);
}

ESP32WiFiMQTT::~ESP32WiFiMQTT() {
    cleanup();
}

bool ESP32WiFiMQTT::initialize() {
    if (initialized_) {
        ESP_LOGW(TAG, "이미 초기화됨");
        return true;
    }
    
    ESP_LOGI(TAG, "ESP32 WiFi/MQTT 초기화 시작");
    
    // 이벤트 그룹 생성
    wifi_event_group_ = xEventGroupCreate();
    mqtt_event_group_ = xEventGroupCreate();
    
    if (!wifi_event_group_ || !mqtt_event_group_) {
        ESP_LOGE(TAG, "이벤트 그룹 생성 실패");
        return false;
    }
    
    // WiFi 초기화
    if (!initializeWiFi()) {
        ESP_LOGE(TAG, "WiFi 초기화 실패");
        return false;
    }
    
    // MQTT 초기화
    if (!initializeMQTT()) {
        ESP_LOGE(TAG, "MQTT 초기화 실패");
        return false;
    }
    
    initialized_ = true;
    ESP_LOGI(TAG, "ESP32 WiFi/MQTT 초기화 완료");
    return true;
}

void ESP32WiFiMQTT::cleanup() {
    if (!initialized_) return;
    
    disconnectMQTT();
    disconnectWiFi();
    
    cleanupMQTT();
    cleanupWiFi();
    
    if (wifi_event_group_) {
        vEventGroupDelete(wifi_event_group_);
        wifi_event_group_ = nullptr;
    }
    
    if (mqtt_event_group_) {
        vEventGroupDelete(mqtt_event_group_);
        mqtt_event_group_ = nullptr;
    }
    
    initialized_ = false;
    ESP_LOGI(TAG, "ESP32 WiFi/MQTT 정리 완료");
}

bool ESP32WiFiMQTT::connectWiFi(const std::string& ssid, const std::string& password) {
    if (!initialized_) {
        ESP_LOGE(TAG, "초기화되지 않음");
        return false;
    }
    
    if (wifi_connected_) {
        ESP_LOGW(TAG, "이미 WiFi 연결됨");
        return true;
    }
    
    ESP_LOGI(TAG, "WiFi 연결 시작: %s", ssid.c_str());
    
    // WiFi 설정
    wifi_config_t wifi_config = {};
    strncpy((char*)wifi_config.sta.ssid, ssid.c_str(), sizeof(wifi_config.sta.ssid) - 1);
    strncpy((char*)wifi_config.sta.password, password.c_str(), sizeof(wifi_config.sta.password) - 1);
    
    esp_err_t ret = esp_wifi_set_config(WIFI_IF_STA, &wifi_config);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "WiFi 설정 실패: %s", esp_err_to_name(ret));
        return false;
    }
    
    // WiFi 연결 시작
    ret = esp_wifi_connect();
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "WiFi 연결 시작 실패: %s", esp_err_to_name(ret));
        return false;
    }
    
    // 연결 완료 대기
    EventBits_t bits = xEventGroupWaitBits(wifi_event_group_,
                                          WIFI_CONNECTED_BIT | WIFI_FAIL_BIT,
                                          pdFALSE,
                                          pdFALSE,
                                          pdMS_TO_TICKS(wifi_config_.timeout_ms));
    
    if (bits & WIFI_CONNECTED_BIT) {
        ESP_LOGI(TAG, "WiFi 연결 성공");
        wifi_connected_ = true;
        updateWiFiStatistics();
        return true;
    } else if (bits & WIFI_FAIL_BIT) {
        ESP_LOGE(TAG, "WiFi 연결 실패");
        return false;
    } else {
        ESP_LOGE(TAG, "WiFi 연결 타임아웃");
        return false;
    }
}

void ESP32WiFiMQTT::disconnectWiFi() {
    if (!wifi_connected_) return;
    
    ESP_LOGI(TAG, "WiFi 연결 해제");
    esp_wifi_disconnect();
    wifi_connected_ = false;
    updateWiFiStatistics();
}

bool ESP32WiFiMQTT::isWiFiConnected() const {
    return wifi_connected_;
}

bool ESP32WiFiMQTT::connectMQTT(const std::string& broker, int port) {
    if (!initialized_) {
        ESP_LOGE(TAG, "초기화되지 않음");
        return false;
    }
    
    if (!wifi_connected_) {
        ESP_LOGE(TAG, "WiFi 연결 안됨");
        return false;
    }
    
    if (mqtt_connected_) {
        ESP_LOGW(TAG, "이미 MQTT 연결됨");
        return true;
    }
    
    ESP_LOGI(TAG, "MQTT 연결 시작: %s:%d", broker.c_str(), port);
    
    // MQTT 브로커 설정
    esp_mqtt_client_config_t mqtt_cfg = {};
    mqtt_cfg.broker.address.uri = broker.c_str();
    mqtt_cfg.broker.address.port = port;
    mqtt_cfg.credentials.client_id = mqtt_config_.client_id.c_str();
    mqtt_cfg.credentials.username = mqtt_config_.username.c_str();
    mqtt_cfg.credentials.authentication.password = mqtt_config_.password.c_str();
    mqtt_cfg.session.keepalive = mqtt_config_.keepalive;
    mqtt_cfg.session.disable_clean_session = !mqtt_config_.clean_session;
    
    esp_err_t ret = esp_mqtt_client_set_config(mqtt_client_, &mqtt_cfg);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "MQTT 설정 실패: %s", esp_err_to_name(ret));
        return false;
    }
    
    // MQTT 연결 시작
    ret = esp_mqtt_client_start(mqtt_client_);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "MQTT 연결 시작 실패: %s", esp_err_to_name(ret));
        return false;
    }
    
    // 연결 완료 대기
    EventBits_t bits = xEventGroupWaitBits(mqtt_event_group_,
                                          MQTT_CONNECTED_BIT | MQTT_FAIL_BIT,
                                          pdFALSE,
                                          pdFALSE,
                                          pdMS_TO_TICKS(10000));
    
    if (bits & MQTT_CONNECTED_BIT) {
        ESP_LOGI(TAG, "MQTT 연결 성공");
        mqtt_connected_ = true;
        updateMQTTStatistics();
        return true;
    } else if (bits & MQTT_FAIL_BIT) {
        ESP_LOGE(TAG, "MQTT 연결 실패");
        return false;
    } else {
        ESP_LOGE(TAG, "MQTT 연결 타임아웃");
        return false;
    }
}

void ESP32WiFiMQTT::disconnectMQTT() {
    if (!mqtt_connected_) return;
    
    ESP_LOGI(TAG, "MQTT 연결 해제");
    esp_mqtt_client_stop(mqtt_client_);
    mqtt_connected_ = false;
    updateMQTTStatistics();
}

bool ESP32WiFiMQTT::isMQTTConnected() const {
    return mqtt_connected_;
}

bool ESP32WiFiMQTT::subscribe(const std::string& topic) {
    if (!mqtt_connected_) {
        ESP_LOGE(TAG, "MQTT 연결 안됨");
        return false;
    }
    
    ESP_LOGI(TAG, "MQTT 구독: %s", topic.c_str());
    
    int msg_id = esp_mqtt_client_subscribe(mqtt_client_, topic.c_str(), 0);
    if (msg_id == -1) {
        ESP_LOGE(TAG, "MQTT 구독 실패");
        return false;
    }
    
    ESP_LOGI(TAG, "MQTT 구독 성공: %s (msg_id: %d)", topic.c_str(), msg_id);
    return true;
}

bool ESP32WiFiMQTT::publish(const std::string& topic, const std::string& message) {
    if (!mqtt_connected_) {
        ESP_LOGE(TAG, "MQTT 연결 안됨");
        return false;
    }
    
    ESP_LOGI(TAG, "MQTT 발행: %s -> %s", topic.c_str(), message.c_str());
    
    int msg_id = esp_mqtt_client_publish(mqtt_client_, topic.c_str(), message.c_str(), 0, 1, 0);
    if (msg_id == -1) {
        ESP_LOGE(TAG, "MQTT 발행 실패");
        return false;
    }
    
    ESP_LOGI(TAG, "MQTT 발행 성공: %s (msg_id: %d)", topic.c_str(), msg_id);
    
    // 통계 업데이트
    portENTER_CRITICAL(&stats_mutex_);
    stats_.mqtt_messages_sent++;
    stats_.last_mqtt_activity = esp_timer_get_time();
    portEXIT_CRITICAL(&stats_mutex_);
    
    return true;
}

void ESP32WiFiMQTT::setWiFiCallback(std::function<void(bool)> callback) {
    wifi_callback_ = callback;
}

void ESP32WiFiMQTT::setMQTTMessageCallback(std::function<void(const std::string&, const std::string&)> callback) {
    mqtt_message_callback_ = callback;
}

void ESP32WiFiMQTT::setMQTTConnectionCallback(std::function<void(bool)> callback) {
    mqtt_connection_callback_ = callback;
}

void ESP32WiFiMQTT::setWiFiConfig(const WiFiConfig& config) {
    wifi_config_ = config;
}

void ESP32WiFiMQTT::setMQTTConfig(const MQTTConfig& config) {
    mqtt_config_ = config;
}

std::string ESP32WiFiMQTT::getWiFiIP() const {
    if (!wifi_connected_) return "";
    
    esp_netif_ip_info_t ip_info;
    esp_err_t ret = esp_netif_get_ip_info(netif_, &ip_info);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "IP 정보 가져오기 실패: %s", esp_err_to_name(ret));
        return "";
    }
    
    char ip_str[16];
    snprintf(ip_str, sizeof(ip_str), IPSTR, IP2STR(&ip_info.ip));
    return std::string(ip_str);
}

int ESP32WiFiMQTT::getWiFiRSSI() const {
    if (!wifi_connected_) return 0;
    
    wifi_ap_record_t ap_info;
    esp_err_t ret = esp_wifi_sta_get_ap_info(&ap_info);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "AP 정보 가져오기 실패: %s", esp_err_to_name(ret));
        return 0;
    }
    
    return ap_info.rssi;
}

ESP32WiFiMQTT::Statistics ESP32WiFiMQTT::getStatistics() const {
    portENTER_CRITICAL(&stats_mutex_);
    Statistics stats = stats_;
    portEXIT_CRITICAL(&stats_mutex_);
    return stats;
}

void ESP32WiFiMQTT::wifiEventHandler(void* arg, esp_event_base_t event_base, int32_t event_id, void* event_data) {
    ESP32WiFiMQTT* client = static_cast<ESP32WiFiMQTT*>(arg);
    if (!client) return;
    
    switch (event_id) {
        case WIFI_EVENT_STA_START:
            ESP_LOGI(TAG, "WiFi STA 시작");
            break;
            
        case WIFI_EVENT_STA_CONNECTED:
            ESP_LOGI(TAG, "WiFi 연결됨");
            break;
            
        case WIFI_EVENT_STA_DISCONNECTED:
            ESP_LOGI(TAG, "WiFi 연결 해제됨");
            client->wifi_connected_ = false;
            client->updateWiFiStatistics();
            xEventGroupSetBits(client->wifi_event_group_, WIFI_FAIL_BIT);
            
            if (client->wifi_callback_) {
                client->wifi_callback_(false);
            }
            
            // 재연결 시도
            if (client->wifi_config_.max_retry > 0) {
                ESP_LOGI(TAG, "WiFi 재연결 시도");
                esp_wifi_connect();
            }
            break;
            
        default:
            break;
    }
}

void ESP32WiFiMQTT::mqttEventHandler(void* arg, esp_event_base_t event_base, int32_t event_id, void* event_data) {
    ESP32WiFiMQTT* client = static_cast<ESP32WiFiMQTT*>(arg);
    if (!client) return;
    
    switch (event_id) {
        case MQTT_EVENT_CONNECTED:
            ESP_LOGI(TAG, "MQTT 연결됨");
            client->mqtt_connected_ = true;
            client->updateMQTTStatistics();
            xEventGroupSetBits(client->mqtt_event_group_, MQTT_CONNECTED_BIT);
            
            if (client->mqtt_connection_callback_) {
                client->mqtt_connection_callback_(true);
            }
            break;
            
        case MQTT_EVENT_DISCONNECTED:
            ESP_LOGI(TAG, "MQTT 연결 해제됨");
            client->mqtt_connected_ = false;
            client->updateMQTTStatistics();
            xEventGroupSetBits(client->mqtt_event_group_, MQTT_FAIL_BIT);
            
            if (client->mqtt_connection_callback_) {
                client->mqtt_connection_callback_(false);
            }
            break;
            
        case MQTT_EVENT_DATA:
            mqttDataHandler(arg, static_cast<esp_mqtt_event_handle_t>(event_data));
            break;
            
        default:
            break;
    }
}

void ESP32WiFiMQTT::mqttDataHandler(void* arg, esp_mqtt_event_handle_t event) {
    ESP32WiFiMQTT* client = static_cast<ESP32WiFiMQTT*>(arg);
    if (!client || !event) return;
    
    ESP_LOGI(TAG, "MQTT 메시지 수신: %s", event->topic);
    
    std::string topic(event->topic, event->topic_len);
    std::string message(event->data, event->data_len);
    
    // 통계 업데이트
    portENTER_CRITICAL(&client->stats_mutex_);
    client->stats_.mqtt_messages_received++;
    client->stats_.last_mqtt_activity = esp_timer_get_time();
    portEXIT_CRITICAL(&client->stats_mutex_);
    
    // 콜백 호출
    if (client->mqtt_message_callback_) {
        client->mqtt_message_callback_(topic, message);
    }
}

bool ESP32WiFiMQTT::initializeWiFi() {
    ESP_LOGI(TAG, "WiFi 초기화 시작");
    
    // 이벤트 루프 초기화
    esp_err_t ret = esp_event_loop_create_default();
    if (ret != ESP_OK && ret != ESP_ERR_INVALID_STATE) {
        ESP_LOGE(TAG, "이벤트 루프 생성 실패: %s", esp_err_to_name(ret));
        return false;
    }
    
    // 네트워크 인터페이스 생성
    netif_ = esp_netif_create_default_wifi_sta();
    if (!netif_) {
        ESP_LOGE(TAG, "네트워크 인터페이스 생성 실패");
        return false;
    }
    
    // WiFi 초기화
    ret = esp_wifi_init(&wifi_init_config_t());
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "WiFi 초기화 실패: %s", esp_err_to_name(ret));
        return false;
    }
    
    // WiFi 모드 설정
    ret = esp_wifi_set_mode(WIFI_MODE_STA);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "WiFi 모드 설정 실패: %s", esp_err_to_name(ret));
        return false;
    }
    
    // 이벤트 핸들러 등록
    ret = esp_event_handler_instance_register(WIFI_EVENT, ESP_EVENT_ANY_ID, &wifiEventHandler, this, nullptr);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "WiFi 이벤트 핸들러 등록 실패: %s", esp_err_to_name(ret));
        return false;
    }
    
    ret = esp_event_handler_instance_register(IP_EVENT, IP_EVENT_STA_GOT_IP, &wifiEventHandler, this, nullptr);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "IP 이벤트 핸들러 등록 실패: %s", esp_err_to_name(ret));
        return false;
    }
    
    // WiFi 시작
    ret = esp_wifi_start();
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "WiFi 시작 실패: %s", esp_err_to_name(ret));
        return false;
    }
    
    ESP_LOGI(TAG, "WiFi 초기화 완료");
    return true;
}

void ESP32WiFiMQTT::cleanupWiFi() {
    if (netif_) {
        esp_netif_destroy_default_wifi(netif_);
        netif_ = nullptr;
    }
    
    esp_wifi_stop();
    esp_wifi_deinit();
}

bool ESP32WiFiMQTT::initializeMQTT() {
    ESP_LOGI(TAG, "MQTT 초기화 시작");
    
    // MQTT 클라이언트 생성
    esp_mqtt_client_config_t mqtt_cfg = {};
    mqtt_client_ = esp_mqtt_client_init(&mqtt_cfg);
    if (!mqtt_client_) {
        ESP_LOGE(TAG, "MQTT 클라이언트 생성 실패");
        return false;
    }
    
    // MQTT 이벤트 핸들러 등록
    esp_err_t ret = esp_mqtt_client_register_event(mqtt_client_, ESP_EVENT_ANY_ID, &mqttEventHandler, this);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "MQTT 이벤트 핸들러 등록 실패: %s", esp_err_to_name(ret));
        return false;
    }
    
    ESP_LOGI(TAG, "MQTT 초기화 완료");
    return true;
}

void ESP32WiFiMQTT::cleanupMQTT() {
    if (mqtt_client_) {
        esp_mqtt_client_destroy(mqtt_client_);
        mqtt_client_ = nullptr;
    }
}

void ESP32WiFiMQTT::updateWiFiStatistics() {
    portENTER_CRITICAL(&stats_mutex_);
    stats_.wifi_connected = wifi_connected_;
    if (wifi_connected_) {
        stats_.wifi_ip = getWiFiIP();
        stats_.wifi_rssi = getWiFiRSSI();
    } else {
        stats_.wifi_ip = "";
        stats_.wifi_rssi = 0;
    }
    portEXIT_CRITICAL(&stats_mutex_);
}

void ESP32WiFiMQTT::updateMQTTStatistics() {
    portENTER_CRITICAL(&stats_mutex_);
    stats_.mqtt_connected = mqtt_connected_;
    portEXIT_CRITICAL(&stats_mutex_);
}

void ESP32WiFiMQTT::logMessage(const std::string& message) {
    ESP_LOGI(TAG, "%s", message.c_str());
}
