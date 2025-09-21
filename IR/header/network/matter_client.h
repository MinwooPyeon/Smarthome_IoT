#pragma once

#include <string>
#include <vector>
#include <map>
#include <functional>
#include <iostream>


enum class MatterDeviceType {
    UNKNOWN = 0,
    LIGHT = 1,
    SWITCH = 2,
    THERMOSTAT = 3,
    DOOR_LOCK = 4,
    WINDOW_COVERING = 5,
    MEDIA_PLAYBACK = 6,
    AIR_CONDITIONER = 7,
    AIR_PURIFIER = 8,
    FAN = 9,
    PROJECTOR = 10
};


enum class MatterCommandType {
    UNKNOWN = 0,
    POWER_TOGGLE = 1,
    POWER_ON = 2,
    POWER_OFF = 3,
    BRIGHTNESS_SET = 4,
    TEMPERATURE_SET = 5,
    MODE_CHANGE = 6,
    VOLUME_SET = 7,
    CHANNEL_SET = 8,
    PLAY_PAUSE = 9,
    STOP = 10,
    NEXT = 11,
    PREVIOUS = 12
};


struct MatterCommand {
    std::string device_id;
    MatterCommandType type;
    std::map<std::string, std::string> parameters;
    std::string correlation_id;
    int priority;
    std::string timestamp;
};


struct MatterCommandResult {
    bool success;
    std::string device_id;
    std::string correlation_id;
    std::string status;
    std::string error_message;
    std::map<std::string, std::string> response_data;
};


struct MatterDevice {
    std::string device_id;
    std::string name;
    MatterDeviceType type;
    std::string manufacturer;
    std::string model;
    std::string firmware_version;
    bool online;
    std::map<std::string, std::string> attributes;
    std::vector<std::string> supported_commands;
};


struct DeviceStatus {
    std::string device_id;
    bool online;
    std::map<std::string, std::string> attributes;
    std::string last_update;
};


class MatterClient {
public:
    MatterClient();
    MatterClient(const MatterClient& other);
    MatterClient(MatterClient&& other) noexcept;
    ~MatterClient();

    MatterClient& operator=(const MatterClient& other);
    MatterClient& operator=(MatterClient&& other) noexcept;


    bool initialize(const std::string& fabric_id, const std::string& node_id);
    bool connect();
    void disconnect();
    bool isConnected() const;


    std::vector<MatterDevice> discoverDevices(int timeout_ms = 5000);
    bool addDevice(const MatterDevice& device);
    bool removeDevice(const std::string& device_id);
    std::vector<MatterDevice> getDevices() const;


    MatterCommandResult sendCommand(const MatterCommand& command);
    MatterCommandResult sendCommand(const std::string& device_id, int command_type);


    bool subscribeToDeviceStatus(const std::string& device_id,
                                std::function<void(const std::string&, const std::map<std::string, std::string>&)> callback);
    void unsubscribeFromDeviceStatus(const std::string& device_id);

    void setDebugMode(bool enabled);
    bool isDebugMode() const;
    std::string getLastError() const;

    bool network_connected_;

    std::map<std::string, DeviceStatus> device_statuses_;

    std::string network_address_;

    bool debug_mode_;

private:
    bool initializeMatterNetwork();
    void cleanupMatterNetwork();

    std::string fabric_id_;
    std::string node_id_;
    std::string last_error_;
    std::map<std::string, std::function<void(const std::string&, const std::map<std::string, std::string>&)>> status_callbacks_;
};
