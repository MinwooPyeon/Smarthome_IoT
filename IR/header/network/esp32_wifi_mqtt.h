#ifndef ESP32_WIFI_MQTT_H
#define ESP32_WIFI_MQTT_H

#include <string>
#include <functional>
#include "esp_wifi.h"
#include "esp_event.h"
#include "esp_log.h"
#ifdef ESP32
#endif
#include "freertos/FreeRTOS.h"
#include "freertos/event_groups.h"
#include "freertos/task.h"
#include "stdint.h"

class ESP32WiFiMQTT {
public:
    ESP32WiFiMQTT();
    ~ESP32WiFiMQTT();

    bool connectWiFi(const std::string& ssid, const std::string& password);
    bool isWiFiConnected() const;
    void disconnectWiFi();

    bool connectMQTT(const std::string& broker, int port, const std::string& client_id);
    bool isMQTTConnected() const;
    void disconnectMQTT();

    bool publish(const std::string& topic, const std::string& message);
    bool subscribe(const std::string& topic);
    void setMessageCallback(std::function<void(const std::string&, const std::string&)> callback);

    void loop();

    std::string getWiFiStatus() const;
    std::string getMQTTStatus() const;

private:
        #ifdef ESP32
            void* mqtt_client_;
            void* netif_;
            wifi_config_t wifi_config_;
            void* mqtt_config_;
        #endif
    std::function<void(const std::string&, const std::string&)> message_callback_;

    bool wifi_connected_;
    bool mqtt_connected_;
    bool initialized_;

    EventGroupHandle_t wifi_event_group_;
    EventGroupHandle_t mqtt_event_group_;

    struct Statistics {
        bool wifi_connected;
        bool mqtt_connected;
        int mqtt_messages_sent;
        int mqtt_messages_received;
        uint64_t last_mqtt_activity;
        std::string wifi_ip;
        int wifi_rssi;
    } stats_;

    mutable portMUX_TYPE stats_mutex_;

    std::function<void(bool)> wifi_callback_;
    std::function<void(bool)> mqtt_connection_callback_;
    void onMQTTMessage(char* topic, uint8_t* payload, unsigned int length);

    bool initialize();
    void cleanup();
    bool initializeWiFi();
    void cleanupWiFi();
    bool initializeMQTT();
    void cleanupMQTT();
    void updateWiFiStatistics();
    void updateMQTTStatistics();
    void logMessage(const std::string& message);

    static void wifiEventHandler(void* arg, esp_event_base_t event_base, int32_t event_id, void* event_data);
    static void mqttEventHandler(void* arg, esp_event_base_t event_base, int32_t event_id, void* event_data);
    static void mqttDataHandler(void* arg, int event);

    void setWiFiCallback(std::function<void(bool)> callback);
    void setMQTTMessageCallback(std::function<void(const std::string&, const std::string&)> callback);
    void setMQTTConnectionCallback(std::function<void(bool)> callback);
        #ifdef ESP32
            void setWiFiConfig(const wifi_config_t& config);
            void setMQTTConfig(const void* config);
        #endif

    std::string getWiFiIP() const;
    int getWiFiRSSI() const;
    Statistics getStatistics() const;

    static const char* TAG;
    static const int WIFI_CONNECTED_BIT;
    static const int WIFI_FAIL_BIT;
    static const int MQTT_CONNECTED_BIT;
    static const int MQTT_FAIL_BIT;
};

#endif
