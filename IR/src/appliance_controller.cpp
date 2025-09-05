#include "hardware/appliance_controller.h"
#include "core/platform.h"
#include <iostream>
#include <fstream>
#include <nlohmann/json.hpp>
#include <map>

#ifdef PLATFORM_ESP32
// ESP32 환경에서는 GPIO 라이브러리 사용
#include "driver/gpio.h"
#elif defined(PLATFORM_LINUX)
// Linux 환경에서는 실제 GPIO 사용
#include <wiringPi.h>
#endif

ApplianceController::ApplianceController() {
    initializeIRCodeMapping();
    
    // 기본 가전기기 등록
    registerAppliance("samsung_tv", ApplianceType::TV);
    registerAppliance("samsung_ac", ApplianceType::AIR_CONDITIONER);
    registerAppliance("samsung_purifier", ApplianceType::AIR_PURIFIER);
    registerAppliance("general_projector", ApplianceType::PROJECTOR);
}

ApplianceController::~ApplianceController() {
    // 정리 작업
}

ControlResult ApplianceController::controlAppliance(const std::string& ir_code) {
    std::cout << "IR 코드로 가전기기 제어: " << ir_code << std::endl;
    
    // IR 코드를 제어 명령으로 변환
    ControlCommand command = convertIRToCommand(ir_code);
    std::string appliance_id = getApplianceId(ir_code);
    
    if (command == ControlCommand::UNKNOWN || appliance_id.empty()) {
        return ControlResult(false, "알 수 없는 IR 코드: " + ir_code);
    }
    
    // 실제 제어 실행
    bool success = executeControl(appliance_id, command);
    
    ControlResult result(success, 
                        success ? "제어 성공" : "제어 실패",
                        appliance_id, command);
    
    // 로그 기록
    logControl(appliance_id, command, success);
    
    // 콜백 호출
    if (control_callback_) {
        control_callback_(result);
    }
    
    return result;
}

ControlResult ApplianceController::controlAppliance(const std::string& appliance_id, ControlCommand command) {
    std::cout << "직접 제어: " << appliance_id << " - " << static_cast<int>(command) << std::endl;
    
    if (appliances_.find(appliance_id) == appliances_.end()) {
        return ControlResult(false, "등록되지 않은 가전기기: " + appliance_id);
    }
    
    bool success = executeControl(appliance_id, command);
    
    ControlResult result(success, 
                        success ? "제어 성공" : "제어 실패",
                        appliance_id, command);
    
    // 로그 기록
    logControl(appliance_id, command, success);
    
    // 콜백 호출
    if (control_callback_) {
        control_callback_(result);
    }
    
    return result;
}

ControlCommand ApplianceController::convertIRToCommand(const std::string& ir_code) {
    auto it = ir_code_map_.find(ir_code);
    if (it != ir_code_map_.end()) {
        return it->second.second;
    }
    return ControlCommand::UNKNOWN;
}

std::string ApplianceController::getApplianceId(const std::string& ir_code) {
    auto it = ir_code_map_.find(ir_code);
    if (it != ir_code_map_.end()) {
        return it->second.first;
    }
    return "";
}

bool ApplianceController::registerAppliance(const std::string& appliance_id, ApplianceType type) {
    appliances_[appliance_id] = type;
    std::cout << "가전기기 등록: " << appliance_id << std::endl;
    return true;
}

bool ApplianceController::unregisterAppliance(const std::string& appliance_id) {
    auto it = appliances_.find(appliance_id);
    if (it != appliances_.end()) {
        appliances_.erase(it);
        std::cout << "가전기기 해제: " << appliance_id << std::endl;
        return true;
    }
    return false;
}

std::vector<std::string> ApplianceController::getRegisteredAppliances() const {
    std::vector<std::string> result;
    for (const auto& pair : appliances_) {
        result.push_back(pair.first);
    }
    return result;
}

ApplianceType ApplianceController::getApplianceType(const std::string& appliance_id) const {
    auto it = appliances_.find(appliance_id);
    if (it != appliances_.end()) {
        return it->second;
    }
    return ApplianceType::UNKNOWN;
}

void ApplianceController::setControlCallback(std::function<void(const ControlResult&)> callback) {
    control_callback_ = callback;
}

bool ApplianceController::loadConfiguration(const std::string& config_file) {
    try {
        std::ifstream file(config_file);
        if (!file.is_open()) {
            std::cerr << "설정 파일을 열 수 없음: " << config_file << std::endl;
            return false;
        }
        
        nlohmann::json config;
        file >> config;
        
        // 가전기기 정보 로드
        if (config.contains("appliances")) {
            for (const auto& appliance : config["appliances"]) {
                std::string id = appliance["id"];
                std::string type_str = appliance["type"];
                
                ApplianceType type = ApplianceType::UNKNOWN;
                if (type_str == "TV") type = ApplianceType::TV;
                else if (type_str == "AIR_CONDITIONER") type = ApplianceType::AIR_CONDITIONER;
                else if (type_str == "AIR_PURIFIER") type = ApplianceType::AIR_PURIFIER;
                else if (type_str == "PROJECTOR") type = ApplianceType::PROJECTOR;
                
                registerAppliance(id, type);
            }
        }
        
        std::cout << "설정 파일 로드 완료: " << config_file << std::endl;
        return true;
        
    } catch (const std::exception& e) {
        std::cerr << "설정 파일 로드 실패: " << e.what() << std::endl;
        return false;
    }
}

bool ApplianceController::saveConfiguration(const std::string& config_file) {
    try {
        nlohmann::json config;
        
        // 가전기기 정보 저장
        nlohmann::json appliances_array = nlohmann::json::array();
        for (const auto& pair : appliances_) {
            nlohmann::json appliance;
            appliance["id"] = pair.first;
            
            std::string type_str;
            switch (pair.second) {
                case ApplianceType::TV: type_str = "TV"; break;
                case ApplianceType::AIR_CONDITIONER: type_str = "AIR_CONDITIONER"; break;
                case ApplianceType::AIR_PURIFIER: type_str = "AIR_PURIFIER"; break;
                case ApplianceType::PROJECTOR: type_str = "PROJECTOR"; break;
                default: type_str = "UNKNOWN"; break;
            }
            appliance["type"] = type_str;
            
            appliances_array.push_back(appliance);
        }
        config["appliances"] = appliances_array;
        
        // 파일에 저장
        std::ofstream file(config_file);
        file << config.dump(4);
        
        std::cout << "설정 파일 저장 완료: " << config_file << std::endl;
        return true;
        
    } catch (const std::exception& e) {
        std::cerr << "설정 파일 저장 실패: " << e.what() << std::endl;
        return false;
    }
}

void ApplianceController::initializeIRCodeMapping() {
    // Samsung TV IR 코드 매핑
    ir_code_map_["0xE0E040BF"] = {"samsung_tv", ControlCommand::POWER_TOGGLE};
    ir_code_map_["0xE0E0E01F"] = {"samsung_tv", ControlCommand::VOLUME_UP};
    ir_code_map_["0xE0E0D02F"] = {"samsung_tv", ControlCommand::VOLUME_DOWN};
    ir_code_map_["0xE0E048B7"] = {"samsung_tv", ControlCommand::CHANNEL_UP};
    ir_code_map_["0xE0E008F7"] = {"samsung_tv", ControlCommand::CHANNEL_DOWN};
    
    // Samsung Air Conditioner IR 코드 매핑
    ir_code_map_["0xE0E040BF"] = {"samsung_ac", ControlCommand::POWER_TOGGLE};
    ir_code_map_["0xE0E014EB"] = {"samsung_ac", ControlCommand::MODE_CHANGE};
    ir_code_map_["0xE0E018E7"] = {"samsung_ac", ControlCommand::TEMP_SET};
    ir_code_map_["0xE0E01CE3"] = {"samsung_ac", ControlCommand::TEMP_UP};
    ir_code_map_["0xE0E05CA3"] = {"samsung_ac", ControlCommand::TEMP_DOWN};
    
    // Samsung Air Purifier IR 코드 매핑
    ir_code_map_["0xE0E040BF"] = {"samsung_purifier", ControlCommand::POWER_TOGGLE};
    ir_code_map_["0xE0E014EB"] = {"samsung_purifier", ControlCommand::MODE_CHANGE};
    ir_code_map_["0xE0E0F50A"] = {"samsung_purifier", ControlCommand::FAN_SPEED};
    
    // General Projector IR 코드 매핑
    ir_code_map_["0x20DF10EF"] = {"general_projector", ControlCommand::POWER_TOGGLE};
    ir_code_map_["0x20DF50AF"] = {"general_projector", ControlCommand::MODE_CHANGE};
    
    std::cout << "IR 코드 매핑 초기화 완료: " << ir_code_map_.size() << "개 코드" << std::endl;
}

bool ApplianceController::executeControl(const std::string& appliance_id, ControlCommand command) {
    LOG_INFO("제어 실행: %s - %d", appliance_id.c_str(), static_cast<int>(command));
    
#ifdef PLATFORM_ESP32
    // ESP32: 실제 GPIO 제어
    int gpio_pin = getGPIOForAppliance(appliance_id);
    if (gpio_pin >= 0) {
        return controlGPIO(gpio_pin, true);
    }
    return false;
#elif defined(PLATFORM_WINDOWS)
    // Windows 시뮬레이션
    LOG_INFO("[시뮬레이션] %s 제어: %d", appliance_id.c_str(), static_cast<int>(command));
    return true;
#elif defined(PLATFORM_LINUX)
    // Linux: 실제 GPIO 제어
    int gpio_pin = getGPIOForAppliance(appliance_id);
    if (gpio_pin >= 0) {
        return controlGPIO(gpio_pin, true);
    }
    return false;
#endif
}

bool ApplianceController::controlGPIO(int gpio_pin, bool state) {
#ifdef PLATFORM_ESP32
    // ESP32 GPIO 제어
    gpio_set_direction(static_cast<gpio_num_t>(gpio_pin), GPIO_OUTPUT);
    gpio_set_level(static_cast<gpio_num_t>(gpio_pin), state ? GPIO_HIGH : GPIO_LOW);
    LOG_INFO("ESP32 GPIO %d 제어: %s", gpio_pin, state ? "HIGH" : "LOW");
    return true;
#elif defined(PLATFORM_LINUX)
    // Linux GPIO 제어
    digitalWrite(gpio_pin, state ? HIGH : LOW);
    LOG_INFO("Linux GPIO %d 제어: %s", gpio_pin, state ? "HIGH" : "LOW");
    return true;
#elif defined(PLATFORM_WINDOWS)
    // Windows 시뮬레이션
    LOG_INFO("Windows GPIO 시뮬레이션 %d 제어: %s", gpio_pin, state ? "HIGH" : "LOW");
    return true;
#endif
}

void ApplianceController::logControl(const std::string& appliance_id, ControlCommand command, bool success) {
    std::string status = success ? "성공" : "실패";
    std::cout << "[로그] " << appliance_id << " 제어 " << status 
              << " - 명령: " << static_cast<int>(command) << std::endl;
}

// 헬퍼 함수: 가전기기별 GPIO 핀 반환
int getGPIOForAppliance(const std::string& appliance_id) {
    static std::map<std::string, int> gpio_map = {
        {"samsung_tv", 24},
        {"samsung_ac", 25},
        {"samsung_purifier", 26},
        {"general_projector", 27}
    };
    
    auto it = gpio_map.find(appliance_id);
    return (it != gpio_map.end()) ? it->second : -1;
}
