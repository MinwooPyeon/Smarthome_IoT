#pragma once

#include <string>
#include <map>
#include <vector>
#include <functional>
#include <memory>

// 가전기기 타입
enum class ApplianceType {
    TV,
    AIR_CONDITIONER,
    AIR_PURIFIER,
    PROJECTOR,
    UNKNOWN
};

// 제어 명령
enum class ControlCommand {
    POWER_ON,
    POWER_OFF,
    POWER_TOGGLE,
    VOLUME_UP,
    VOLUME_DOWN,
    CHANNEL_UP,
    CHANNEL_DOWN,
    MODE_CHANGE,
    TEMP_UP,
    TEMP_DOWN,
    TEMP_SET,
    FAN_SPEED,
    UNKNOWN
};

// 제어 결과
struct ControlResult {
    bool success;
    std::string message;
    std::string appliance_id;
    ControlCommand command;
    
    ControlResult(bool s = false, const std::string& msg = "", 
                  const std::string& id = "", ControlCommand cmd = ControlCommand::UNKNOWN)
        : success(s), message(msg), appliance_id(id), command(cmd) {}
};

// 전방 선언
class IRLearner;
class IRDatabase;
class IRProtocolDetector;
class GenericDeviceManager;
class MqttClient;

/**
 * @brief 가전기기 제어 클래스
 * 
 * IR 신호를 통한 가전기기 제어를 담당합니다.
 * ESP32, Linux, Windows 플랫폼을 지원합니다.
 */
class ApplianceController {
public:
    ApplianceController();
    ApplianceController(class IRReceiver* ir_receiver); // IR 수신기와 연동
    ~ApplianceController();
    
    // 가전기기 제어
    ControlResult controlAppliance(const std::string& ir_code);
    ControlResult controlAppliance(const std::string& appliance_id, ControlCommand command);
    
    // 가전기기 등록/해제
    bool registerAppliance(const std::string& appliance_id, ApplianceType type);
    bool unregisterAppliance(const std::string& appliance_id);
    std::vector<std::string> getRegisteredAppliances() const;
    ApplianceType getApplianceType(const std::string& appliance_id) const;
    
    // 콜백 설정
    void setControlCallback(std::function<void(const ControlResult&)> callback);
    
    // 설정 관리
    bool loadConfiguration(const std::string& config_file);
    bool saveConfiguration(const std::string& config_file);
    
    // IR 학습 기능
    bool startIRLearning(const std::string& appliance_id, const std::string& command_name);
    bool stopIRLearning();
    bool isIRLearning() const;
    std::vector<std::string> getLearnedCommands(const std::string& appliance_id) const;
    
    // IR 코드 검색
    std::string findIRCode(const std::string& appliance_id, const std::string& command) const;
    
    // MQTT 통합 메서드
    void setMqttClient(class MqttClient* mqtt_client);
    void handleMqttCommand(const std::string& topic, const std::string& message);
    void publishStatus(const std::string& appliance_id, const std::string& status);
    void publishIRCode(const std::string& appliance_id, const std::string& command, const std::string& ir_code);
    
    // 범용 기기 관리자 연동
    void setGenericDeviceManager(class GenericDeviceManager* generic_device_manager);
    bool registerGenericDevice(const std::string& device_id, const std::string& device_name, const std::string& device_type);
    std::vector<std::string> getGenericDevices();

private:
    std::map<std::string, ApplianceType> appliances_;
    std::map<std::string, std::pair<std::string, ControlCommand>> ir_code_map_;
    std::function<void(const ControlResult&)> control_callback_;
    
    // IR 학습 시스템
    std::unique_ptr<IRLearner> ir_learner_;
    std::unique_ptr<IRDatabase> ir_database_;
    std::unique_ptr<IRProtocolDetector> protocol_detector_;
    
    // MQTT 클라이언트
    class MqttClient* mqtt_client_;
    
    // 범용 기기 관리자
    class GenericDeviceManager* generic_device_manager_;
    
    void initializeIRCodeMapping();
    void updateIRCodeMapping();
    ControlCommand convertIRToCommand(const std::string& ir_code);
    std::string getApplianceId(const std::string& ir_code);
    bool executeControl(const std::string& appliance_id, ControlCommand command);
};

#endif // APPLIANCE_CONTROLLER_H