#include "hardware/appliance_controller.h"
#include "hardware/ir_learner.h"
#include "hardware/ir_database.h"
#include "hardware/ir_protocol_detector.h"
#include "core/platform.h"
#include <iostream>
#include <fstream>
#include "ArduinoJson.h"
#include <map>

#ifdef PLATFORM_ESP32
#include "driver/gpio.h"
#elif defined(PLATFORM_LINUX)
#include <wiringPi.h>
#endif

ApplianceController::ApplianceController() {
    // IR 학습 시스템 초기화
    ir_learner_ = std::make_unique<IRLearner>();
    ir_database_ = std::make_unique<IRDatabase>();
    protocol_detector_ = std::make_unique<IRProtocolDetector>();
    
    // MQTT 클라이언트 초기화
    mqtt_client_ = nullptr;
    
    // IR 데이터베이스 초기화
    ir_database_->initialize();
    
    // 기본 가전기기 등록
    registerAppliance("samsung_tv", ApplianceType::TV);
    registerAppliance("samsung_ac", ApplianceType::AIR_CONDITIONER);
    registerAppliance("samsung_purifier", ApplianceType::AIR_PURIFIER);
    registerAppliance("general_projector", ApplianceType::PROJECTOR);
    
    initializeIRCodeMapping();
}

ApplianceController::ApplianceController(IRReceiver* ir_receiver) {
    ir_learner_ = std::make_unique<IRLearner>(ir_receiver);
    ir_database_ = std::make_unique<IRDatabase>();
    protocol_detector_ = std::make_unique<IRProtocolDetector>();
    
    // MQTT 클라이언트 초기화
    mqtt_client_ = nullptr;
    
    // IR 데이터베이스 초기화
    ir_database_->initialize();
    
    // 기본 가전기기 등록
    registerAppliance("samsung_tv", ApplianceType::TV);
    registerAppliance("samsung_ac", ApplianceType::AIR_CONDITIONER);
    registerAppliance("samsung_purifier", ApplianceType::AIR_PURIFIER);
    registerAppliance("general_projector", ApplianceType::PROJECTOR);
    
    initializeIRCodeMapping();
    
    LOG_INFO("ApplianceController 초기화 완료 (IR 수신기 연동)");
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
    LOG_DEBUG("IR 코드 변환 시도: %s", ir_code.c_str());
    
    // 1. 기존 맵핑에서 찾기
    auto it = ir_code_map_.find(ir_code);
    if (it != ir_code_map_.end()) {
        LOG_DEBUG("기존 맵핑에서 발견: %s -> %d", ir_code.c_str(), static_cast<int>(it->second.second));
        return it->second.second;
    }
    
    // 2. IR 데이터베이스에서 찾기
    if (ir_database_) {
        auto entries = ir_database_->searchByIRCode(ir_code);
        if (!entries.empty()) {
            const auto& entry = entries[0];
            ControlCommand command = ControlCommand::UNKNOWN;
            
            // 명령어 매핑
            if (entry.command == "power") command = ControlCommand::POWER_TOGGLE;
            else if (entry.command == "volume_up") command = ControlCommand::VOLUME_UP;
            else if (entry.command == "volume_down") command = ControlCommand::VOLUME_DOWN;
            else if (entry.command == "channel_up") command = ControlCommand::CHANNEL_UP;
            else if (entry.command == "channel_down") command = ControlCommand::CHANNEL_DOWN;
            else if (entry.command == "mute") command = ControlCommand::MUTE;
            else if (entry.command == "input") command = ControlCommand::INPUT_SELECT;
            else if (entry.command == "menu") command = ControlCommand::MENU;
            else if (entry.command == "ok") command = ControlCommand::OK;
            else if (entry.command == "back") command = ControlCommand::BACK;
            else if (entry.command == "temp_up") command = ControlCommand::TEMP_UP;
            else if (entry.command == "temp_down") command = ControlCommand::TEMP_DOWN;
            else if (entry.command == "mode") command = ControlCommand::MODE;
            else if (entry.command == "fan_speed") command = ControlCommand::FAN_SPEED;
            else if (entry.command == "swing") command = ControlCommand::SWING;
            else if (entry.command == "timer") command = ControlCommand::TIMER;
            else if (entry.command == "sleep") command = ControlCommand::SLEEP;
            
            if (command != ControlCommand::UNKNOWN) {
                // 맵핑에 추가
                std::string appliance_id = entry.brand + "_" + entry.device_type;
                ir_code_map_[ir_code] = {appliance_id, command};
                LOG_INFO("데이터베이스에서 발견하여 맵핑 추가: %s -> %s (%d)", 
                        ir_code.c_str(), appliance_id.c_str(), static_cast<int>(command));
                return command;
            }
        }
    }
    
    // 3. 학습된 코드에서 찾기
    if (ir_learner_) {
        auto learned_codes = ir_learner_->getLearnedCodes();
        for (const auto& learned : learned_codes) {
            if (learned.ir_code == ir_code) {
                ControlCommand command = ControlCommand::UNKNOWN;
                
                // 학습된 명령어 매핑
                if (learned.command_name == "power") command = ControlCommand::POWER_TOGGLE;
                else if (learned.command_name == "volume_up") command = ControlCommand::VOLUME_UP;
                else if (learned.command_name == "volume_down") command = ControlCommand::VOLUME_DOWN;
                else if (learned.command_name == "channel_up") command = ControlCommand::CHANNEL_UP;
                else if (learned.command_name == "channel_down") command = ControlCommand::CHANNEL_DOWN;
                else if (learned.command_name == "mute") command = ControlCommand::MUTE;
                else if (learned.command_name == "input") command = ControlCommand::INPUT_SELECT;
                else if (learned.command_name == "menu") command = ControlCommand::MENU;
                else if (learned.command_name == "ok") command = ControlCommand::OK;
                else if (learned.command_name == "back") command = ControlCommand::BACK;
                else if (learned.command_name == "temp_up") command = ControlCommand::TEMP_UP;
                else if (learned.command_name == "temp_down") command = ControlCommand::TEMP_DOWN;
                else if (learned.command_name == "mode") command = ControlCommand::MODE;
                else if (learned.command_name == "fan_speed") command = ControlCommand::FAN_SPEED;
                else if (learned.command_name == "swing") command = ControlCommand::SWING;
                else if (learned.command_name == "timer") command = ControlCommand::TIMER;
                else if (learned.command_name == "sleep") command = ControlCommand::SLEEP;
                
                if (command != ControlCommand::UNKNOWN) {
                    // 맵핑에 추가
                    ir_code_map_[ir_code] = {learned.appliance_id, command};
                    LOG_INFO("학습된 코드에서 발견하여 맵핑 추가: %s -> %s (%d)", 
                            ir_code.c_str(), learned.appliance_id.c_str(), static_cast<int>(command));
                    return command;
                }
            }
        }
    }
    
    LOG_WARN("알 수 없는 IR 코드: %s", ir_code.c_str());
    return ControlCommand::UNKNOWN;
}

std::string ApplianceController::getApplianceId(const std::string& ir_code) {
    LOG_DEBUG("기기 ID 조회 시도: %s", ir_code.c_str());
    
    // 1. 기존 맵핑에서 찾기
    auto it = ir_code_map_.find(ir_code);
    if (it != ir_code_map_.end()) {
        LOG_DEBUG("기존 맵핑에서 발견: %s -> %s", ir_code.c_str(), it->second.first.c_str());
        return it->second.first;
    }
    
    // 2. IR 데이터베이스에서 찾기
    if (ir_database_) {
        auto entries = ir_database_->searchByIRCode(ir_code);
        if (!entries.empty()) {
            const auto& entry = entries[0];
            std::string appliance_id = entry.brand + "_" + entry.device_type;
            LOG_INFO("데이터베이스에서 발견: %s -> %s", ir_code.c_str(), appliance_id.c_str());
            return appliance_id;
        }
    }
    
    // 3. 학습된 코드에서 찾기
    if (ir_learner_) {
        auto learned_codes = ir_learner_->getLearnedCodes();
        for (const auto& learned : learned_codes) {
            if (learned.ir_code == ir_code) {
                LOG_INFO("학습된 코드에서 발견: %s -> %s", ir_code.c_str(), learned.appliance_id.c_str());
                return learned.appliance_id;
            }
        }
    }
    
    // 4. 범용 기기에서 찾기 (GenericDeviceManager 사용)
    // TODO: GenericDeviceManager와 연동
    
    LOG_WARN("알 수 없는 IR 코드의 기기 ID: %s", ir_code.c_str());
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
        
        DynamicJsonDocument doc(2048);
        DeserializationError error = deserializeJson(doc, file);
        
        if (error) {
            std::cerr << "JSON 파싱 오류: " << error.c_str() << std::endl;
            return false;
        }
        
        // 가전기기 정보 로드
        if (doc.containsKey("appliances")) {
            JsonArray appliances = doc["appliances"];
            for (JsonObject appliance : appliances) {
                std::string id = appliance["id"].as<std::string>();
                std::string type_str = appliance["type"].as<std::string>();
                
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
        DynamicJsonDocument doc(2048);
        
        // 가전기기 정보 저장
        JsonArray appliances_array = doc.createNestedArray("appliances");
        for (const auto& pair : appliances_) {
            JsonObject appliance = appliances_array.createNestedObject();
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
        }
        
        // 파일에 저장
        std::ofstream file(config_file);
        serializeJsonPretty(doc, file);
        
        std::cout << "설정 파일 저장 완료: " << config_file << std::endl;
        return true;
        
    } catch (const std::exception& e) {
        std::cerr << "설정 파일 저장 실패: " << e.what() << std::endl;
        return false;
    }
}

void ApplianceController::initializeIRCodeMapping() {
    LOG_INFO("IR 코드 매핑 초기화 - 동적 학습 시스템 활성화");
    
    ir_code_map_.clear();
    
    // 1. IR 데이터베이스에서 기본 맵핑 로드
    if (ir_database_) {
        std::vector<std::string> brands = {"samsung", "lg", "sony", "panasonic", "philips", "toshiba", "sharp", "jvc"};
        std::vector<std::string> commands = {"power", "volume_up", "volume_down", "channel_up", "channel_down", 
                                            "mute", "input", "menu", "ok", "back", "temp_up", "temp_down", 
                                            "mode", "fan_speed", "swing", "timer", "sleep"};
        
        for (const auto& brand : brands) {
            auto entries = ir_database_->searchByBrand(brand);
            for (const auto& entry : entries) {
                std::string key = entry.ir_code;
                std::string appliance_id = brand + "_" + entry.device_type;
                ControlCommand command = ControlCommand::UNKNOWN;
                
                // 명령어 매핑
                if (entry.command == "power") command = ControlCommand::POWER_TOGGLE;
                else if (entry.command == "volume_up") command = ControlCommand::VOLUME_UP;
                else if (entry.command == "volume_down") command = ControlCommand::VOLUME_DOWN;
                else if (entry.command == "channel_up") command = ControlCommand::CHANNEL_UP;
                else if (entry.command == "channel_down") command = ControlCommand::CHANNEL_DOWN;
                else if (entry.command == "mute") command = ControlCommand::MUTE;
                else if (entry.command == "input") command = ControlCommand::INPUT_SELECT;
                else if (entry.command == "menu") command = ControlCommand::MENU;
                else if (entry.command == "ok") command = ControlCommand::OK;
                else if (entry.command == "back") command = ControlCommand::BACK;
                else if (entry.command == "temp_up") command = ControlCommand::TEMP_UP;
                else if (entry.command == "temp_down") command = ControlCommand::TEMP_DOWN;
                else if (entry.command == "mode") command = ControlCommand::MODE;
                else if (entry.command == "fan_speed") command = ControlCommand::FAN_SPEED;
                else if (entry.command == "swing") command = ControlCommand::SWING;
                else if (entry.command == "timer") command = ControlCommand::TIMER;
                else if (entry.command == "sleep") command = ControlCommand::SLEEP;
                
                if (command != ControlCommand::UNKNOWN) {
                    ir_code_map_[key] = {appliance_id, command};
                }
            }
        }
    }
    
    // 2. 학습된 IR 코드 로드
    if (ir_learner_) {
        auto learned_codes = ir_learner_->getLearnedCodes();
        for (const auto& learned : learned_codes) {
            std::string key = learned.ir_code;
            std::string appliance_id = learned.appliance_id;
            ControlCommand command = ControlCommand::UNKNOWN;
            
            // 학습된 명령어 매핑
            if (learned.command_name == "power") command = ControlCommand::POWER_TOGGLE;
            else if (learned.command_name == "volume_up") command = ControlCommand::VOLUME_UP;
            else if (learned.command_name == "volume_down") command = ControlCommand::VOLUME_DOWN;
            else if (learned.command_name == "channel_up") command = ControlCommand::CHANNEL_UP;
            else if (learned.command_name == "channel_down") command = ControlCommand::CHANNEL_DOWN;
            else if (learned.command_name == "mute") command = ControlCommand::MUTE;
            else if (learned.command_name == "input") command = ControlCommand::INPUT_SELECT;
            else if (learned.command_name == "menu") command = ControlCommand::MENU;
            else if (learned.command_name == "ok") command = ControlCommand::OK;
            else if (learned.command_name == "back") command = ControlCommand::BACK;
            else if (learned.command_name == "temp_up") command = ControlCommand::TEMP_UP;
            else if (learned.command_name == "temp_down") command = ControlCommand::TEMP_DOWN;
            else if (learned.command_name == "mode") command = ControlCommand::MODE;
            else if (learned.command_name == "fan_speed") command = ControlCommand::FAN_SPEED;
            else if (learned.command_name == "swing") command = ControlCommand::SWING;
            else if (learned.command_name == "timer") command = ControlCommand::TIMER;
            else if (learned.command_name == "sleep") command = ControlCommand::SLEEP;
            
            if (command != ControlCommand::UNKNOWN) {
                ir_code_map_[key] = {appliance_id, command};
                LOG_INFO("학습된 코드 맵핑 추가: %s -> %s (%s)", 
                        key.c_str(), appliance_id.c_str(), learned.command_name.c_str());
            }
        }
    }
    
    LOG_INFO("동적 IR 매핑 완료: %d개 코드 로드됨", ir_code_map_.size());
    LOG_INFO("IR 학습 모드 활성화 - 실제 리모컨에서 코드 학습 가능");
}

void ApplianceController::updateIRCodeMapping() {
    LOG_INFO("IR 코드 맵핑 업데이트 시작");
    
    // 학습된 IR 코드만 다시 로드
    if (ir_learner_) {
        auto learned_codes = ir_learner_->getLearnedCodes();
        int added_count = 0;
        
        for (const auto& learned : learned_codes) {
            std::string key = learned.ir_code;
            std::string appliance_id = learned.appliance_id;
            ControlCommand command = ControlCommand::UNKNOWN;
            
            // 학습된 명령어 매핑
            if (learned.command_name == "power") command = ControlCommand::POWER_TOGGLE;
            else if (learned.command_name == "volume_up") command = ControlCommand::VOLUME_UP;
            else if (learned.command_name == "volume_down") command = ControlCommand::VOLUME_DOWN;
            else if (learned.command_name == "channel_up") command = ControlCommand::CHANNEL_UP;
            else if (learned.command_name == "channel_down") command = ControlCommand::CHANNEL_DOWN;
            else if (learned.command_name == "mute") command = ControlCommand::MUTE;
            else if (learned.command_name == "input") command = ControlCommand::INPUT_SELECT;
            else if (learned.command_name == "menu") command = ControlCommand::MENU;
            else if (learned.command_name == "ok") command = ControlCommand::OK;
            else if (learned.command_name == "back") command = ControlCommand::BACK;
            else if (learned.command_name == "temp_up") command = ControlCommand::TEMP_UP;
            else if (learned.command_name == "temp_down") command = ControlCommand::TEMP_DOWN;
            else if (learned.command_name == "mode") command = ControlCommand::MODE;
            else if (learned.command_name == "fan_speed") command = ControlCommand::FAN_SPEED;
            else if (learned.command_name == "swing") command = ControlCommand::SWING;
            else if (learned.command_name == "timer") command = ControlCommand::TIMER;
            else if (learned.command_name == "sleep") command = ControlCommand::SLEEP;
            
            if (command != ControlCommand::UNKNOWN) {
                auto it = ir_code_map_.find(key);
                if (it == ir_code_map_.end() || it->second.first != appliance_id || it->second.second != command) {
                    ir_code_map_[key] = {appliance_id, command};
                    added_count++;
                    LOG_INFO("맵핑 업데이트: %s -> %s (%s)", 
                            key.c_str(), appliance_id.c_str(), learned.command_name.c_str());
                }
            }
        }
        
        LOG_INFO("IR 코드 맵핑 업데이트 완료: %d개 추가/업데이트됨", added_count);
    }
}

// MQTT 통합 메서드들
void ApplianceController::setMqttClient(MqttClient* mqtt_client) {
    mqtt_client_ = mqtt_client;
    LOG_INFO("MQTT 클라이언트 설정 완료");
}

void ApplianceController::handleMqttCommand(const std::string& topic, const std::string& message) {
    LOG_INFO("MQTT 명령 처리: %s -> %s", topic.c_str(), message.c_str());
    
    try {
        DynamicJsonDocument doc(1024);
        DeserializationError error = deserializeJson(doc, message);
        
        if (error) {
            LOG_ERROR("JSON 파싱 오류: %s", error.c_str());
            return;
        }
        
        if (topic == "irremote/command") {
            // IR 제어 명령
            if (doc.containsKey("device_id") && doc.containsKey("command")) {
                std::string device_id = doc["device_id"];
                std::string command = doc["command"];
                
                // IR 코드 찾기
                std::string ir_code = findIRCode(device_id, command);
                if (!ir_code.empty()) {
                    ControlResult result = controlAppliance(ir_code);
                    publishStatus(device_id, result.success ? "success" : "failed");
                } else {
                    LOG_ERROR("IR 코드를 찾을 수 없음: %s - %s", device_id.c_str(), command.c_str());
                    publishStatus(device_id, "code_not_found");
                }
            }
        } else if (topic == "irremote/learn") {
            // IR 학습 명령
            if (doc.containsKey("device_id") && doc.containsKey("command")) {
                std::string device_id = doc["device_id"];
                std::string command = doc["command"];
                
                bool success = startIRLearning(device_id, command);
                publishStatus(device_id, success ? "learning_started" : "learning_failed");
            }
        } else if (topic == "irremote/status") {
            // 상태 조회 명령
            if (doc.containsKey("device_id")) {
                std::string device_id = doc["device_id"];
                auto commands = getLearnedCommands(device_id);
                
                DynamicJsonDocument response(1024);
                response["device_id"] = device_id;
                response["status"] = "online";
                response["learned_commands"] = commands.size();
                
                JsonArray commands_array = response.createNestedArray("commands");
                for (const auto& cmd : commands) {
                    commands_array.add(cmd);
                }
                
                std::string response_str;
                serializeJson(response, response_str);
                
                if (mqtt_client_) {
                    mqtt_client_->publish("irremote/response", response_str);
                }
            }
        }
        
    } catch (const std::exception& e) {
        LOG_ERROR("MQTT 명령 처리 오류: %s", e.what());
    }
}

void ApplianceController::publishStatus(const std::string& appliance_id, const std::string& status) {
    if (!mqtt_client_) return;
    
    DynamicJsonDocument doc(256);
    doc["device_id"] = appliance_id;
    doc["status"] = status;
    doc["timestamp"] = esp_timer_get_time() / 1000;
    
    std::string message;
    serializeJson(doc, message);
    
    mqtt_client_->publish("irremote/response", message);
    LOG_INFO("상태 발행: %s -> %s", appliance_id.c_str(), status.c_str());
}

void ApplianceController::publishIRCode(const std::string& appliance_id, const std::string& command, const std::string& ir_code) {
    if (!mqtt_client_) return;
    
    DynamicJsonDocument doc(256);
    doc["device_id"] = appliance_id;
    doc["command"] = command;
    doc["ir_code"] = ir_code;
    doc["timestamp"] = esp_timer_get_time() / 1000;
    
    std::string message;
    serializeJson(doc, message);
    
    mqtt_client_->publish("irremote/learned_code", message);
    LOG_INFO("IR 코드 발행: %s - %s -> %s", appliance_id.c_str(), command.c_str(), ir_code.c_str());
}

bool ApplianceController::executeControl(const std::string& appliance_id, ControlCommand command) {
    LOG_INFO("제어 실행: %s - %d", appliance_id.c_str(), static_cast<int>(command));
    
#ifdef PLATFORM_ESP32
    int gpio_pin = getGPIOForAppliance(appliance_id);
    if (gpio_pin >= 0) {
        return controlGPIO(gpio_pin, true);
    }
    return false;
#elif defined(PLATFORM_WINDOWS)
    LOG_INFO("[시뮬레이션] %s 제어: %d", appliance_id.c_str(), static_cast<int>(command));
    return true;
#elif defined(PLATFORM_LINUX)
    int gpio_pin = getGPIOForAppliance(appliance_id);
    if (gpio_pin >= 0) {
        return controlGPIO(gpio_pin, true);
    }
    return false;
#endif
}

bool ApplianceController::controlGPIO(int gpio_pin, bool state) {
#ifdef PLATFORM_ESP32
    gpio_set_direction(static_cast<gpio_num_t>(gpio_pin), GPIO_OUTPUT);
    gpio_set_level(static_cast<gpio_num_t>(gpio_pin), state ? GPIO_HIGH : GPIO_LOW);
    LOG_INFO("ESP32 GPIO %d 제어: %s", gpio_pin, state ? "HIGH" : "LOW");
    return true;
#elif defined(PLATFORM_LINUX)
    digitalWrite(gpio_pin, state ? HIGH : LOW);
    LOG_INFO("Linux GPIO %d 제어: %s", gpio_pin, state ? "HIGH" : "LOW");
    return true;
#elif defined(PLATFORM_WINDOWS)
    LOG_INFO("Windows GPIO 시뮬레이션 %d 제어: %s", gpio_pin, state ? "HIGH" : "LOW");
    return true;
#endif
}

void ApplianceController::logControl(const std::string& appliance_id, ControlCommand command, bool success) {
    std::string status = success ? "성공" : "실패";
    std::cout << "[로그] " << appliance_id << " 제어 " << status 
              << " - 명령: " << static_cast<int>(command) << std::endl;
}

// 가전기기별 GPIO 핀 반환
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

// IR 학습 기능 구현
bool ApplianceController::startIRLearning(const std::string& appliance_id, const std::string& command_name) {
    if (!ir_learner_) {
        LOG_ERROR("IR 학습기가 초기화되지 않음");
        return false;
    }
    
    LOG_INFO("IR 학습 시작: %s - %s", appliance_id.c_str(), command_name.c_str());
    
    // IR 학습 모드 시작
    if (!ir_learner_->startLearningMode()) {
        LOG_ERROR("IR 학습 모드 시작 실패");
        return false;
    }
    
    // IR 코드 학습
    bool success = ir_learner_->learnIRCode(appliance_id, command_name);
    
    if (success) {
        LOG_INFO("IR 코드 학습 성공: %s - %s", appliance_id.c_str(), command_name.c_str());
        
        // 학습 완료 후 맵핑 업데이트
        updateIRCodeMapping();
        
        // MQTT로 학습 완료 알림
        if (mqtt_client_) {
            publishStatus(appliance_id, "learning_completed");
        }
        
        // 학습된 코드를 매핑에 추가
        auto learned_commands = ir_learner_->getLearnedCommands(appliance_id);
        for (const auto& cmd : learned_commands) {
            if (cmd.command_name == command_name) {
                // IR 코드를 제어 명령으로 변환
                ControlCommand control_cmd = ControlCommand::UNKNOWN;
                if (command_name == "power") control_cmd = ControlCommand::POWER_TOGGLE;
                else if (command_name == "volume_up") control_cmd = ControlCommand::VOLUME_UP;
                else if (command_name == "volume_down") control_cmd = ControlCommand::VOLUME_DOWN;
                else if (command_name == "channel_up") control_cmd = ControlCommand::CHANNEL_UP;
                else if (command_name == "channel_down") control_cmd = ControlCommand::CHANNEL_DOWN;
                
                if (control_cmd != ControlCommand::UNKNOWN) {
                    ir_code_map_[cmd.ir_code.code] = {appliance_id, control_cmd};
                    LOG_INFO("IR 코드 매핑 추가: %s -> %s", cmd.ir_code.code.c_str(), command_name.c_str());
                }
                break;
            }
        }
    }
    
    return success;
}

bool ApplianceController::stopIRLearning() {
    if (!ir_learner_) {
        return false;
    }
    
    ir_learner_->stopLearningMode();
    LOG_INFO("IR 학습 모드 중지");
    return true;
}

bool ApplianceController::isIRLearning() const {
    return ir_learner_ ? ir_learner_->isLearningMode() : false;
}

std::vector<std::string> ApplianceController::getLearnedCommands(const std::string& appliance_id) const {
    if (!ir_learner_) {
        return {};
    }
    
    auto learned_commands = ir_learner_->getLearnedCommands(appliance_id);
    std::vector<std::string> command_names;
    
    for (const auto& cmd : learned_commands) {
        command_names.push_back(cmd.command_name);
    }
    
    return command_names;
}

std::string ApplianceController::findIRCode(const std::string& appliance_id, const std::string& command) const {
    // 먼저 로컬 매핑에서 검색
    for (const auto& pair : ir_code_map_) {
        if (pair.second.first == appliance_id) {
            // 명령어 매칭 확인
            ControlCommand control_cmd = pair.second.second;
            std::string expected_command = "";
            
            switch (control_cmd) {
                case ControlCommand::POWER_TOGGLE: expected_command = "power"; break;
                case ControlCommand::VOLUME_UP: expected_command = "volume_up"; break;
                case ControlCommand::VOLUME_DOWN: expected_command = "volume_down"; break;
                case ControlCommand::CHANNEL_UP: expected_command = "channel_up"; break;
                case ControlCommand::CHANNEL_DOWN: expected_command = "channel_down"; break;
                default: break;
            }
            
            if (expected_command == command) {
                return pair.first;
            }
        }
    }
    
    // IR 데이터베이스에서 검색
    if (ir_database_) {
        auto entries = ir_database_->searchByBrand(appliance_id.substr(0, appliance_id.find('_')));
        for (const auto& entry : entries) {
            if (entry.command == command) {
                return entry.ir_code;
            }
        }
    }
    
    return "";
}
