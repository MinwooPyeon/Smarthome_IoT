#ifndef APPLIANCE_CONTROLLER_H
#define APPLIANCE_CONTROLLER_H

#include <string>
#include <map>
#include <vector>
#include <functional>

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

class ApplianceController {
public:
    ApplianceController();
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

private:
    std::map<std::string, ApplianceType> appliances_;
    std::map<std::string, std::pair<std::string, ControlCommand>> ir_code_map_;
    std::function<void(const ControlResult&)> control_callback_;
    
    void initializeIRCodeMapping();
    ControlCommand convertIRToCommand(const std::string& ir_code);
    std::string getApplianceId(const std::string& ir_code);
    bool executeControl(const std::string& appliance_id, ControlCommand command);
    bool controlGPIO(int gpio_pin, bool state);
    void logControl(const std::string& appliance_id, ControlCommand command, bool success);
};

#endif // APPLIANCE_CONTROLLER_H
