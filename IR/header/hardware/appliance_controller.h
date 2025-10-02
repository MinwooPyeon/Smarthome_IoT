#ifndef APPLIANCE_CONTROLLER_H
#define APPLIANCE_CONTROLLER_H

#include <string>
#include <map>
#include <vector>
#include <functional>
#include <memory>
#include "network/mqtt_client.h"

enum class ApplianceType {
    TV,
    AIR_CONDITIONER,
    AIR_PURIFIER,
    PROJECTOR,
    UNKNOWN
};

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

struct ControlResult {
    bool success;
    std::string message;
    std::string appliance_id;
    ControlCommand command;

    ControlResult(bool s = false, const std::string& msg = "",
                  const std::string& id = "", ControlCommand cmd = ControlCommand::UNKNOWN)
        : success(s), message(msg), appliance_id(id), command(cmd) {}
};

class IRLearner;
class IRDatabase;
class IRProtocolDetector;
class GenericDeviceManager;

class ApplianceController {
public:
    ApplianceController();
    ApplianceController(class IRReceiver* ir_receiver);
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

    bool startIRLearning(const std::string& appliance_id, const std::string& command_name);
    bool stopIRLearning();
    bool isIRLearning() const;
    std::vector<std::string> getLearnedCommands(const std::string& appliance_id) const;

    std::string findIRCode(const std::string& appliance_id, const std::string& command) const;

    void setMqttClient(class MqttClient* mqtt_client);
    void handleMqttCommand(const std::string& topic, const std::string& message);
    void publishStatus(const std::string& appliance_id, const std::string& status);
    void publishIRCode(const std::string& appliance_id, const std::string& command, const std::string& ir_code);

    void setGenericDeviceManager(class GenericDeviceManager* generic_device_manager);
    bool registerGenericDevice(const std::string& device_id, const std::string& device_name, const std::string& device_type);
    std::vector<std::string> getGenericDevices();

private:
    std::map<std::string, ApplianceType> appliances_;
    std::map<std::string, std::pair<std::string, ControlCommand>> ir_code_map_;
    std::function<void(const ControlResult&)> control_callback_;

    std::unique_ptr<IRLearner> ir_learner_;
    std::unique_ptr<IRDatabase> ir_database_;
    std::unique_ptr<IRProtocolDetector> protocol_detector_;

    class MqttClient* mqtt_client_;

    class GenericDeviceManager* generic_device_manager_;

    void initializeIRCodeMapping();
    void updateIRCodeMapping();
    ControlCommand convertIRToCommand(const std::string& ir_code);
    std::string getApplianceId(const std::string& ir_code);
    bool executeControl(const std::string& appliance_id, ControlCommand command);
};

#endif
