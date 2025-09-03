#ifndef APPLIANCE_CONTROLLER_H
#define APPLIANCE_CONTROLLER_H

#include <string>
#include <map>
#include <vector>
#include <functional>

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

class ApplianceController {
public:
    ApplianceController();
    ~ApplianceController();
    
    // 가전기기 제어
    ControlResult controlAppliance(const std::string& ir_code);
    ControlResult controlAppliance(const std::string& appliance_id, ControlCommand command);
    
    // IR 코드를 제어 명령으로 변환
    ControlCommand convertIRToCommand(const std::string& ir_code);
    std::string getApplianceId(const std::string& ir_code);
    
    // 가전기기 등록/해제
    bool registerAppliance(const std::string& appliance_id, ApplianceType type);
    bool unregisterAppliance(const std::string& appliance_id);
    
    // 가전기기 목록 조회
    std::vector<std::string> getRegisteredAppliances() const;
    ApplianceType getApplianceType(const std::string& appliance_id) const;
    
    // 콜백 설정
    void setControlCallback(std::function<void(const ControlResult&)> callback);
    
    // 설정 로드/저장
    bool loadConfiguration(const std::string& config_file);
    bool saveConfiguration(const std::string& config_file);

private:
    // IR 코드 매핑 테이블
    std::map<std::string, std::pair<std::string, ControlCommand>> ir_code_map_;
    
    // 가전기기 정보
    std::map<std::string, ApplianceType> appliances_;
    
    // 제어 콜백
    std::function<void(const ControlResult&)> control_callback_;
    
    // IR 코드 매핑 초기화
    void initializeIRCodeMapping();
    
    // 실제 하드웨어 제어
    bool executeControl(const std::string& appliance_id, ControlCommand command);
    
    // GPIO 제어 (릴레이 등)
    bool controlGPIO(int gpio_pin, bool state);
    
    // 로그 기록
    void logControl(const std::string& appliance_id, ControlCommand command, bool success);
};

#endif // APPLIANCE_CONTROLLER_H
