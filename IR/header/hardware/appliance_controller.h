#ifndef APPLIANCE_CONTROLLER_H
#define APPLIANCE_CONTROLLER_H

#include <string>
#include <map>
#include <vector>
#include <functional>
#include <memory>

enum class ApplianceType {
    UNKNOWN,
    TV,
    AIR_CONDITIONER,
    AIR_PURIFIER,
    PROJECTOR
};

enum class ControlCommand {
    UNKNOWN,
    POWER_TOGGLE,
    VOLUME_UP,
    VOLUME_DOWN,
    CHANNEL_UP,
    CHANNEL_DOWN,
    MODE_CHANGE,
    TEMP_UP,
    TEMP_DOWN,
    TEMP_SET,
    FAN_SPEED
};

struct ControlResult {
    bool success;
    std::string message;
    std::string appliance_id;
    ControlCommand command;
    
    ControlResult(bool s, const std::string& msg, 
                  const std::string& id = "", ControlCommand cmd = ControlCommand::UNKNOWN)
        : success(s), message(msg), appliance_id(id), command(cmd) {}
};

// 전방 선언
class IRLearner;
class IRDatabase;
class IRProtocolDetector;

class ApplianceController {
public:
    ApplianceController();
    ApplianceController(IRReceiver* ir_receiver); // IR 수신기와 연동
    ~ApplianceController();
    
    ControlResult controlAppliance(const std::string& ir_code);
    ControlResult controlAppliance(const std::string& appliance_id, ControlCommand command);
    
    bool registerAppliance(const std::string& appliance_id, ApplianceType type);
    bool unregisterAppliance(const std::string& appliance_id);
    std::vector<std::string> getRegisteredAppliances() const;
    ApplianceType getApplianceType(const std::string& appliance_id) const;
    
    void setControlCallback(std::function<void(const ControlResult&)> callback);
    
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
    
    void initializeIRCodeMapping();
    void updateIRCodeMapping();
    ControlCommand convertIRToCommand(const std::string& ir_code);
    std::string getApplianceId(const std::string& ir_code);
    bool executeControl(const std::string& appliance_id, ControlCommand command);
    bool controlGPIO(int gpio_pin, bool state);
    void logControl(const std::string& appliance_id, ControlCommand command, bool success);
};

#endif 
