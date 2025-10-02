#include "hardware/appliance_controller.h"
#include <iostream>
#include <fstream>
#include <ArduinoJson.h>
#include <map>
#include <ctime>

#ifdef _WIN32
#elif defined(ESP_PLATFORM)
#include "driver/gpio.h"
#else
#include <wiringPi.h>
#endif

struct IRCodeEntry {
    std::string ir_code;
    std::string brand;
    std::string device_type;
    std::string command;
};

struct LearnedIRCode {
    std::string ir_code;
    std::string appliance_id;
    std::string command;
};

class IRLearner {
private:
    bool learning_mode_ = false;
    std::string current_appliance_id_;
    std::string current_command_;
    std::vector<struct LearnedIRCode> learned_codes_;

public:
    IRLearner() = default;
    IRLearner(class IRReceiver* ir_receiver) {}

    std::vector<struct LearnedIRCode> getLearnedCodes() const {
        return learned_codes_;
    }

    bool startLearning(const std::string& appliance_id, const std::string& command) {
        if (learning_mode_) {
            std::cerr << "이미 학습 모드가 활성화되어 있음" << std::endl;
            return false;
        }

        learning_mode_ = true;
        current_appliance_id_ = appliance_id;
        current_command_ = command;

        std::cout << "IR 학습 시작: " << appliance_id << " - " << command << std::endl;

        return true;
    }

    void stopLearning() {
        if (learning_mode_) {
            learning_mode_ = false;
            current_appliance_id_.clear();
            current_command_.clear();
            std::cout << "IR 학습 중지" << std::endl;
        }
    }

    bool isLearning() const {
        return learning_mode_;
    }

    void addLearnedCode(const std::string& ir_code) {
        if (learning_mode_ && !current_appliance_id_.empty() && !current_command_.empty()) {
            LearnedIRCode learned;
            learned.ir_code = ir_code;
            learned.appliance_id = current_appliance_id_;
            learned.command = current_command_;

            learned_codes_.push_back(learned);

            std::cout << "IR 코드 학습 완료: " << ir_code << " -> "
                      << current_appliance_id_ << " - " << current_command_ << std::endl;

            stopLearning();
        }
    }
};

class IRDatabase {
private:
    std::vector<struct IRCodeEntry> database_;

public:
    void initialize() {
        IRCodeEntry entry;

        entry.ir_code = "0xE0E040BF";
        entry.brand = "Samsung";
        entry.device_type = "TV";
        entry.command = "power";
        database_.push_back(entry);

        entry.ir_code = "0xE0E0E01F";
        entry.brand = "Samsung";
        entry.device_type = "TV";
        entry.command = "volume_up";
        database_.push_back(entry);

        entry.ir_code = "0xE0E0D02F";
        entry.brand = "Samsung";
        entry.device_type = "TV";
        entry.command = "volume_down";
        database_.push_back(entry);

        entry.ir_code = "0xE0E048B7";
        entry.brand = "Samsung";
        entry.device_type = "TV";
        entry.command = "channel_up";
        database_.push_back(entry);

        entry.ir_code = "0xE0E008F7";
        entry.brand = "Samsung";
        entry.device_type = "TV";
        entry.command = "channel_down";
        database_.push_back(entry);

        std::cout << "IR 데이터베이스 초기화 완료: " << database_.size() << "개 코드" << std::endl;
    }

    std::vector<struct IRCodeEntry> searchByIRCode(const std::string& ir_code) const {
        std::vector<struct IRCodeEntry> results;

        for (const auto& entry : database_) {
            if (entry.ir_code == ir_code) {
                results.push_back(entry);
            }
        }

        if (results.empty()) {
            std::cout << "IR 코드를 찾을 수 없음: " << ir_code << std::endl;
        } else {
            std::cout << "IR 코드 검색 결과: " << results.size() << "개 매치" << std::endl;
        }

        return results;
    }

    std::vector<struct IRCodeEntry> searchByBrand(const std::string& brand) const {
        std::vector<struct IRCodeEntry> results;

        for (const auto& entry : database_) {
            if (entry.brand == brand) {
                results.push_back(entry);
            }
        }

        return results;
    }

    std::vector<struct IRCodeEntry> searchByCommand(const std::string& command) const {
        std::vector<struct IRCodeEntry> results;

        for (const auto& entry : database_) {
            if (entry.command == command) {
                results.push_back(entry);
            }
        }

        return results;
    }
};

class IRProtocolDetector {
public:
    std::string detectProtocol(const std::string& ir_code) const {
        if (ir_code.empty()) {
            return "UNKNOWN";
        }

        if (ir_code.length() == 10 && ir_code.substr(0, 2) == "0x") {
            return "NEC";
        } else if (ir_code.length() == 8 && ir_code.substr(0, 2) == "0x") {
            return "RC5";
        } else if (ir_code.length() == 6 && ir_code.substr(0, 2) == "0x") {
            return "Sony";
        } else if (ir_code.find(',') != std::string::npos) {
            return "RAW";
        } else {
            return "UNKNOWN";
        }
    }

    int getCodeLength(const std::string& ir_code) const {
        if (ir_code.empty()) {
            return 0;
        }

        if (ir_code.substr(0, 2) == "0x") {
            return (ir_code.length() - 2) * 4;
        } else if (ir_code.find(',') != std::string::npos) {
            int count = 1;
            for (char c : ir_code) {
                if (c == ',') count++;
            }
            return count;
        }

        return 0;
    }

    bool isValidIRCode(const std::string& ir_code) const {
        if (ir_code.empty()) {
            return false;
        }

        if (ir_code.substr(0, 2) == "0x") {
            for (size_t i = 2; i < ir_code.length(); i++) {
                char c = ir_code[i];
                if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'))) {
                    return false;
                }
            }
            return true;
        }

        for (char c : ir_code) {
            if (!((c >= '0' && c <= '9') || c == ',' || c == ' ')) {
                return false;
            }
        }

        return true;
    }
};

ApplianceController::ApplianceController() {
    ir_learner_ = std::unique_ptr<IRLearner>(new IRLearner());
    ir_database_ = std::unique_ptr<IRDatabase>(new IRDatabase());
    protocol_detector_ = std::unique_ptr<IRProtocolDetector>(new IRProtocolDetector());

    mqtt_client_ = nullptr;

    generic_device_manager_ = nullptr;

    ir_database_->initialize();

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

    mqtt_client_ = nullptr;

    generic_device_manager_ = nullptr;

    ir_database_->initialize();

    registerAppliance("samsung_tv", ApplianceType::TV);
    registerAppliance("samsung_ac", ApplianceType::AIR_CONDITIONER);
    registerAppliance("samsung_purifier", ApplianceType::AIR_PURIFIER);
    registerAppliance("general_projector", ApplianceType::PROJECTOR);

    initializeIRCodeMapping();
}

ApplianceController::~ApplianceController() = default;

ControlResult ApplianceController::controlAppliance(const std::string& ir_code) {
    std::cout << "IR 코드로 제어 시도: " << ir_code << std::endl;

    ControlCommand command = convertIRToCommand(ir_code);
    if (command == ControlCommand::UNKNOWN) {
        return ControlResult(false, "알 수 없는 IR 코드: " + ir_code);
    }

    std::string appliance_id = getApplianceId(ir_code);
    if (appliance_id.empty()) {
        return ControlResult(false, "IR 코드에 해당하는 기기를 찾을 수 없음: " + ir_code);
    }

    return executeControl(appliance_id, command);
}

ControlResult ApplianceController::controlAppliance(const std::string& appliance_id, ControlCommand command) {
    std::cout << "기기 제어 시도: " << appliance_id << " - " << static_cast<int>(command) << std::endl;

    auto it = appliances_.find(appliance_id);
    if (it == appliances_.end()) {
        return ControlResult(false, "등록되지 않은 기기: " + appliance_id);
    }

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

        updateIRCodeMapping();

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

    std::cout << "범용 기기 등록: " << device_name << " (" << device_type << ")" << std::endl;

    if (mqtt_client_) {
        publishStatus(device_id, "device_registered");
    }

    return true;
}

std::vector<std::string> ApplianceController::getGenericDevices() {
    std::vector<std::string> device_list;

    if (generic_device_manager_) {
    }

    return device_list;
}

void ApplianceController::initializeIRCodeMapping() {
}

void ApplianceController::updateIRCodeMapping() {
    if (ir_learner_) {
        auto learned_codes = ir_learner_->getLearnedCodes();
        for (const auto& learned : learned_codes) {
        }
    }
}

ControlCommand ApplianceController::convertIRToCommand(const std::string& ir_code) {
    auto it = ir_code_map_.find(ir_code);
    if (it != ir_code_map_.end()) {
        return it->second.second;
    }

    if (ir_database_) {
        auto entries = ir_database_->searchByIRCode(ir_code);
        if (!entries.empty()) {
            const auto& entry = entries[0];
            if (entry.command == "power") return ControlCommand::POWER_TOGGLE;
            else if (entry.command == "volume_up") return ControlCommand::VOLUME_UP;
            else if (entry.command == "volume_down") return ControlCommand::VOLUME_DOWN;
        }
    }

    if (ir_learner_) {
        auto learned_codes = ir_learner_->getLearnedCodes();
        for (const auto& learned : learned_codes) {
            if (learned.ir_code == ir_code) {
                if (learned.command == "power") return ControlCommand::POWER_TOGGLE;
                else if (learned.command == "volume_up") return ControlCommand::VOLUME_UP;
                else if (learned.command == "volume_down") return ControlCommand::VOLUME_DOWN;
            }
        }
    }

    if (generic_device_manager_) {
    }

    return ControlCommand::UNKNOWN;
}

std::string ApplianceController::getApplianceId(const std::string& ir_code) {
    auto it = ir_code_map_.find(ir_code);
    if (it != ir_code_map_.end()) {
        return it->second.first;
    }

    if (ir_database_) {
        auto entries = ir_database_->searchByIRCode(ir_code);
        if (!entries.empty()) {
            const auto& entry = entries[0];
            return entry.brand + "_" + entry.device_type;
        }
    }

    if (ir_learner_) {
        auto learned_codes = ir_learner_->getLearnedCodes();
        for (const auto& learned : learned_codes) {
            if (learned.ir_code == ir_code) {
                return learned.appliance_id;
            }
        }
    }

    if (generic_device_manager_) {
    }

    return "";
}

bool ApplianceController::executeControl(const std::string& appliance_id, ControlCommand command) {
    std::cout << "제어 실행: " << appliance_id << " - " << static_cast<int>(command) << std::endl;

    bool success = true;
    std::string message = "제어 성공";

    ControlResult result(success, message, appliance_id, command);

    if (control_callback_) {
        control_callback_(result);
    }

    return success;
}
