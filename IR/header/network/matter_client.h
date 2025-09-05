#pragma once

#include <string>
#include <vector>
#include <map>
#include <functional>
#include <iostream>

// Matter 디바이스 타입
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

// Matter 명령 타입
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

// Matter 명령
struct MatterCommand {
    std::string device_id;
    MatterCommandType type;
    std::map<std::string, std::string> parameters;
    std::string correlation_id;
    int priority;
    std::string timestamp;
};

// Matter 명령 결과
struct MatterCommandResult {
    bool success;
    std::string device_id;
    std::string correlation_id;
    std::string status;
    std::string error_message;
    std::map<std::string, std::string> response_data;
};

// Matter 디바이스 정보
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

// 디바이스 상태 정보
struct DeviceStatus {
    std::string device_id;
    bool online;
    std::map<std::string, std::string> attributes;
    std::string last_update;
};

// Matter 클라이언트 클래스
class MatterClient {
public:
    MatterClient();
    MatterClient(const MatterClient& other);
    MatterClient(MatterClient&& other) noexcept;
    ~MatterClient();
    
    MatterClient& operator=(const MatterClient& other);
    MatterClient& operator=(MatterClient&& other) noexcept;
    
    // 기본 초기화 및 연결
    bool initialize(const std::string& fabric_id, const std::string& node_id);
    bool connect();
    void disconnect();
    bool isConnected() const;
    
    // 디바이스 검색 및 관리
    std::vector<MatterDevice> discoverDevices(int timeout_ms = 5000);
    bool addDevice(const MatterDevice& device);
    bool removeDevice(const std::string& device_id);
    std::vector<MatterDevice> getDevices() const;
    
    // 명령 전송
    MatterCommandResult sendCommand(const MatterCommand& command);
    MatterCommandResult sendCommand(const std::string& device_id, int command_type);
    
    // 디바이스 상태 모니터링
    bool subscribeToDeviceStatus(const std::string& device_id, 
                                std::function<void(const std::string&, const std::map<std::string, std::string>&)> callback);
    void unsubscribeFromDeviceStatus(const std::string& device_id);
    
    // 설정 및 상태
    void setDebugMode(bool enabled);
    bool isDebugMode() const;
    std::string getLastError() const;
    
    // Matter 네트워크 연결 상태
    bool network_connected_;
    
    // Matter 디바이스 상태
    std::map<std::string, DeviceStatus> device_statuses_;
    
    // Matter 네트워크 주소
    std::string network_address_;
    
    // 디버그 모드
    bool debug_mode_;

private:
    // Matter 네트워크 초기화
    bool initializeMatterNetwork();
    void cleanupMatterNetwork();
    
    // 내부 상태 변수들
    std::string fabric_id_;
    std::string node_id_;
    std::string last_error_;
    std::map<std::string, std::function<void(const std::string&, const std::map<std::string, std::string>&)>> status_callbacks_;
};
