#include "hardware/appliance_controller.h"
#include <iostream>
#include <fstream>
#include <ArduinoJson.h>
#include <map>
#include <ctime>

#ifdef _WIN32
// Windows 환경에서는 시뮬레이션
#elif defined(ESP_PLATFORM)
// ESP32 환경에서는 GPIO 직접 제어
#include "driver/gpio.h"
#else
// Linux 환경에서는 실제 GPIO 사용
#include <wiringPi.h>
#endif

// 전방 선언 (실제 구현은 별도 파일에서)
class IRLearner {
public:
    IRLearner() = default;
    IRLearner(class IRReceiver* ir_receiver) {}
    std::vector<struct LearnedIRCode> getLearnedCodes() const { return {}; }
    bool startLearning(const std::string& appliance_id, const std::string& command) { return true; }
    void stopLearning() {}
    bool isLearning() const { return false; }
};

class IRDatabase {
public:
    void initialize() {}
    std::vector<struct IRCodeEntry> searchByIRCode(const std::string& ir_code) const { return {}; }
};

class IRProtocolDetector {
public:
    std::string detectProtocol(const std::string& ir_code) const { return "UNKNOWN"; }
};

struct LearnedIRCode {
    std::string ir_code;
    std::string appliance_id;
    std::string command;
};

struct IRCodeEntry {
    std::string ir_code;
    std::string brand;
    std::string device_type;
    std::string command;
};

ApplianceController::ApplianceController() {
    // IR 학습 시스템 초기화
    ir_learner_ = std::unique_ptr<IRLearner>(new IRLearner());
    ir_database_ = std::unique_ptr<IRDatabase>(new IRDatabase());
    protocol_detector_ = std::unique_ptr<IRProtocolDetector>(new IRProtocolDetector());

    // MQTT 클라이언트 초기화
    mqtt_client_ = nullptr;

    // 범용 기기 관리자 초기화
    generic_device_manager_ = nullptr;

    // IR 데이터베이스 초기화
    ir_database_->initialize();

    // 기본 가전기기 등록
    registerAppliance("samsung_tv", ApplianceType::TV);
    registerAppliance("samsung_ac", ApplianceType::AIR_CONDITIONER);
    registerAppliance("samsung_purifier", ApplianceType::AIR_PURIFIER);
    registerAppliance("general_projector", ApplianceType::PROJECTOR);

    initializeIRCodeMapping();
}

ApplianceController::ApplianceController(class IRReceiver* ir_receiver) {
    ir_learner_ = std::unique_ptr<IRLearner>(new IRLearner(ir_receiver));
    ir_database_ = std::unique_ptr<IRDatabase>(new IRDatabase());
    protocol_detector_ = std::unique_ptr<IRProtocolDetector>(new IRProtocolDetector());

    // MQTT 클라이언트 초기화
    mqtt_client_ = nullptr;

    // 범용 기기 관리자 초기화
    generic_device_manager_ = nullptr;

    // IR 데이터베이스 초기화
    ir_database_->initialize();

    // 기본 가전기기 등록
    registerAppliance("samsung_tv", ApplianceType::TV);
    registerAppliance("samsung_ac", ApplianceType::AIR_CONDITIONER);
    registerAppliance("samsung_purifier", ApplianceType::AIR_PURIFIER);
    registerAppliance("general_projector", ApplianceType::PROJECTOR);

    initializeIRCodeMapping();
}

ApplianceController::~ApplianceController() = default;

ControlResult ApplianceController::controlAppliance(const std::string& ir_code) {
    std::cout << "IR 코드로 제어 시도: " << ir_code << std::endl;

    // IR 코드를 명령어로 변환
    ControlCommand command = convertIRToCommand(ir_code);
    if (command == ControlCommand::UNKNOWN) {
        return ControlResult(false, "알 수 없는 IR 코드: " + ir_code);
    }

    // 기기 ID 찾기
    std::string appliance_id = getApplianceId(ir_code);
    if (appliance_id.empty()) {
        return ControlResult(false, "IR 코드에 해당하는 기기를 찾을 수 없음: " + ir_code);
    }

    // 제어 실행
    return executeControl(appliance_id, command);
}

ControlResult ApplianceController::controlAppliance(const std::string& appliance_id, ControlCommand command) {
    std::cout << "기기 제어 시도: " << appliance_id << " - " << static_cast<int>(command) << std::endl;

    // 기기 등록 여부 확인
    auto it = appliances_.find(appliance_id);
    if (it == appliances_.end()) {
        return ControlResult(false, "등록되지 않은 기기: " + appliance_id);
    }

    // 제어 실행
    return executeControl(appliance_id, command);
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
    std::vector<std::string> appliances;
    for (const auto& pair : appliances_) {
        appliances.push_back(pair.first);
    }
    return appliances;
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
    std::ifstream file(config_file);
    if (!file.is_open()) {
        std::cerr << "설정 파일을 열 수 없음: " << config_file << std::endl;
        return false;
    }

    try {
        std::string json_str((std::istreambuf_iterator<char>(file)),
                             std::istreambuf_iterator<char>());

        DynamicJsonDocument doc(2048);
        DeserializationError error = deserializeJson(doc, json_str);

        if (error) {
            std::cerr << "JSON 파싱 오류: " << error.c_str() << std::endl;
            return false;
        }

        // 설정 로드 로직
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

        std::cout << "설정 로드 완료: " << config_file << std::endl;
        return true;
    } catch (const std::exception& e) {
        std::cerr << "설정 파일 파싱 오류: " << e.what() << std::endl;
        return false;
    }
}

bool ApplianceController::saveConfiguration(const std::string& config_file) {
    try {
        DynamicJsonDocument doc(2048);

        // 가전기기 정보 저장
        JsonArray appliances = doc.createNestedArray("appliances");
        for (const auto& pair : appliances_) {
            JsonObject appliance = appliances.createNestedObject();
            appliance["id"] = pair.first.c_str();

            std::string type_str;
            switch (pair.second) {
                case ApplianceType::TV: type_str = "TV"; break;
                case ApplianceType::AIR_CONDITIONER: type_str = "AIR_CONDITIONER"; break;
                case ApplianceType::AIR_PURIFIER: type_str = "AIR_PURIFIER"; break;
                case ApplianceType::PROJECTOR: type_str = "PROJECTOR"; break;
                default: type_str = "UNKNOWN"; break;
            }
            appliance["type"] = type_str.c_str();
        }

        // 파일에 저장
        std::ofstream file(config_file);
        serializeJsonPretty(doc, file);

        std::cout << "설정 저장 완료: " << config_file << std::endl;
        return true;
    } catch (const std::exception& e) {
        std::cerr << "설정 저장 오류: " << e.what() << std::endl;
        return false;
    }
}

bool ApplianceController::startIRLearning(const std::string& appliance_id, const std::string& command_name) {
    if (!ir_learner_) {
        std::cerr << "IR 학습기가 초기화되지 않음" << std::endl;
        return false;
    }

    bool success = ir_learner_->startLearning(appliance_id, command_name);
    if (success) {
        std::cout << "IR 학습 시작: " << appliance_id << " - " << command_name << std::endl;

        // 맵핑 업데이트
        updateIRCodeMapping();

        // MQTT로 상태 발행
        if (mqtt_client_) {
            publishStatus(appliance_id, "learning_started");
        }
    }

    return success;
}

bool ApplianceController::stopIRLearning() {
    if (!ir_learner_) {
        return false;
    }

    ir_learner_->stopLearning();
    std::cout << "IR 학습 중지" << std::endl;
    return true;
}

bool ApplianceController::isIRLearning() const {
    return ir_learner_ ? ir_learner_->isLearning() : false;
}

std::vector<std::string> ApplianceController::getLearnedCommands(const std::string& appliance_id) const {
    std::vector<std::string> commands;

    if (ir_learner_) {
        auto learned_codes = ir_learner_->getLearnedCodes();
        for (const auto& learned : learned_codes) {
            if (learned.appliance_id == appliance_id) {
                commands.push_back(learned.command);
            }
        }
    }

    return commands;
}

std::string ApplianceController::findIRCode(const std::string& appliance_id, const std::string& command) const {
    if (ir_learner_) {
        auto learned_codes = ir_learner_->getLearnedCodes();
        for (const auto& learned : learned_codes) {
            if (learned.appliance_id == appliance_id && learned.command == command) {
                return learned.ir_code;
            }
        }
    }

    return "";
}

void ApplianceController::setMqttClient(class MqttClient* mqtt_client) {
    mqtt_client_ = mqtt_client;
    std::cout << "MQTT 클라이언트 설정 완료" << std::endl;
}

void ApplianceController::handleMqttCommand(const std::string& topic, const std::string& message) {
    try {
        DynamicJsonDocument doc(512);
        DeserializationError error = deserializeJson(doc, message);

        if (error) {
            std::cerr << "MQTT 메시지 파싱 오류: " << error.c_str() << std::endl;
            return;
        }

        if (doc.containsKey("action")) {
            std::string action = doc["action"].as<std::string>();

            if (action == "control") {
                std::string device_id = doc["device_id"].as<std::string>();
                std::string command_str = doc["command"].as<std::string>();

                ControlCommand command = ControlCommand::UNKNOWN;
                if (command_str == "power_toggle") command = ControlCommand::POWER_TOGGLE;
                else if (command_str == "volume_up") command = ControlCommand::VOLUME_UP;
                else if (command_str == "volume_down") command = ControlCommand::VOLUME_DOWN;

                auto result = controlAppliance(device_id, command);

                // 결과를 MQTT로 발행
                if (mqtt_client_) {
                    DynamicJsonDocument response_doc(512);
                    response_doc["device_id"] = device_id.c_str();
                    response_doc["command"] = command_str.c_str();
                    response_doc["success"] = result.success;
                    response_doc["message"] = result.message.c_str();

                    std::string response_str;
                    serializeJson(response_doc, response_str);
                    mqtt_client_->publish("irremote/response", response_str);
                }
            }
            else if (action == "learn") {
                std::string device_id = doc["device_id"].as<std::string>();
                std::string command_name = doc["command"].as<std::string>();

                bool success = startIRLearning(device_id, command_name);

                // 결과를 MQTT로 발행
                if (mqtt_client_) {
                    DynamicJsonDocument response_doc(512);
                    response_doc["action"] = "learn";
                    response_doc["device_id"] = device_id.c_str();
                    response_doc["command"] = command_name.c_str();
                    response_doc["success"] = success;

                    std::string response_str;
                    serializeJson(response_doc, response_str);
                    mqtt_client_->publish("irremote/learn_response", response_str);
                }
            }
        }
    } catch (const std::exception& e) {
        std::cerr << "MQTT 명령 파싱 오류: " << e.what() << std::endl;
    }
}

void ApplianceController::publishStatus(const std::string& appliance_id, const std::string& status) {
    if (!mqtt_client_) return;

    DynamicJsonDocument status_doc(512);
    status_doc["appliance_id"] = appliance_id.c_str();
    status_doc["status"] = status.c_str();
    status_doc["timestamp"] = time(nullptr);

    std::string status_str;
    serializeJson(status_doc, status_str);
    mqtt_client_->publish("irremote/status", status_str);
}

void ApplianceController::publishIRCode(const std::string& appliance_id, const std::string& command, const std::string& ir_code) {
    if (!mqtt_client_) return;

    DynamicJsonDocument ir_doc(512);
    ir_doc["appliance_id"] = appliance_id.c_str();
    ir_doc["command"] = command.c_str();
    ir_doc["ir_code"] = ir_code.c_str();
    ir_doc["timestamp"] = time(nullptr);

    std::string ir_str;
    serializeJson(ir_doc, ir_str);
    mqtt_client_->publish("irremote/learned_code", ir_str);
}

void ApplianceController::setGenericDeviceManager(class GenericDeviceManager* generic_device_manager) {
    generic_device_manager_ = generic_device_manager;
    std::cout << "범용 기기 관리자 설정 완료" << std::endl;
}

bool ApplianceController::registerGenericDevice(const std::string& device_id, const std::string& device_name, const std::string& device_type) {
    if (!generic_device_manager_) {
        std::cerr << "범용 기기 관리자가 설정되지 않음" << std::endl;
        return false;
    }

    // GenericDeviceManager의 registerGenericDevice 메서드 호출
    // 실제 구현은 GenericDeviceManager에서 처리
    std::cout << "범용 기기 등록: " << device_name << " (" << device_type << ")" << std::endl;

    // MQTT로 기기 등록 알림
    if (mqtt_client_) {
        publishStatus(device_id, "device_registered");
    }

    return true;
}

std::vector<std::string> ApplianceController::getGenericDevices() {
    std::vector<std::string> device_list;

    if (generic_device_manager_) {
        // GenericDeviceManager의 getAllDevices 메서드 호출
        // 실제 구현은 GenericDeviceManager에서 처리
    }

    return device_list;
}

void ApplianceController::initializeIRCodeMapping() {
    // 기본 IR 코드 맵핑 초기화
    // 실제 구현에서는 IR 데이터베이스와 학습된 코드를 로드
}

void ApplianceController::updateIRCodeMapping() {
    // 학습된 IR 코드로 맵핑 테이블 업데이트
    if (ir_learner_) {
        auto learned_codes = ir_learner_->getLearnedCodes();
        for (const auto& learned : learned_codes) {
            // 맵핑 테이블 업데이트 로직
        }
    }
}

ControlCommand ApplianceController::convertIRToCommand(const std::string& ir_code) {
    // 1. 기존 맵핑에서 찾기
    auto it = ir_code_map_.find(ir_code);
    if (it != ir_code_map_.end()) {
        return it->second.second;
    }

    // 2. IR 데이터베이스에서 찾기
    if (ir_database_) {
        auto entries = ir_database_->searchByIRCode(ir_code);
        if (!entries.empty()) {
            const auto& entry = entries[0];
            // 명령어 변환 로직
            if (entry.command == "power") return ControlCommand::POWER_TOGGLE;
            else if (entry.command == "volume_up") return ControlCommand::VOLUME_UP;
            else if (entry.command == "volume_down") return ControlCommand::VOLUME_DOWN;
        }
    }

    // 3. 학습된 코드에서 찾기
    if (ir_learner_) {
        auto learned_codes = ir_learner_->getLearnedCodes();
        for (const auto& learned : learned_codes) {
            if (learned.ir_code == ir_code) {
                // 명령어 변환 로직
                if (learned.command == "power") return ControlCommand::POWER_TOGGLE;
                else if (learned.command == "volume_up") return ControlCommand::VOLUME_UP;
                else if (learned.command == "volume_down") return ControlCommand::VOLUME_DOWN;
            }
        }
    }

    // 4. 범용 기기에서 찾기
    if (generic_device_manager_) {
        // GenericDeviceManager에서 IR 코드 검색
        // 실제 구현은 GenericDeviceManager에서 처리
    }

    return ControlCommand::UNKNOWN;
}

std::string ApplianceController::getApplianceId(const std::string& ir_code) {
    // 1. 기존 맵핑에서 찾기
    auto it = ir_code_map_.find(ir_code);
    if (it != ir_code_map_.end()) {
        return it->second.first;
    }

    // 2. IR 데이터베이스에서 찾기
    if (ir_database_) {
        auto entries = ir_database_->searchByIRCode(ir_code);
        if (!entries.empty()) {
            const auto& entry = entries[0];
            return entry.brand + "_" + entry.device_type;
        }
    }

    // 3. 학습된 코드에서 찾기
    if (ir_learner_) {
        auto learned_codes = ir_learner_->getLearnedCodes();
        for (const auto& learned : learned_codes) {
            if (learned.ir_code == ir_code) {
                return learned.appliance_id;
            }
        }
    }

    // 4. 범용 기기에서 찾기
    if (generic_device_manager_) {
        // GenericDeviceManager에서 IR 코드 검색
        // 실제 구현은 GenericDeviceManager에서 처리
    }

    return "";
}

bool ApplianceController::executeControl(const std::string& appliance_id, ControlCommand command) {
    std::cout << "제어 실행: " << appliance_id << " - " << static_cast<int>(command) << std::endl;

    // 실제 제어 로직 (시뮬레이션)
    bool success = true;
    std::string message = "제어 성공";

    ControlResult result(success, message, appliance_id, command);

    // 콜백 호출
    if (control_callback_) {
        control_callback_(result);
    }

    return success;
}
