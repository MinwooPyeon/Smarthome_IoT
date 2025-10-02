#include "hardware/ir_learner.h"
#include "hardware/ir_receiver.h"
#include "core/platform.h"
#include <fstream>
#include <algorithm>
#include <mutex>
#include <ArduinoJson.h>
#include <sstream>
#include "esp_log.h"

static const char* TAG = "IR_LEARNER";

IRLearner::IRLearner() : learning_mode_(false), ir_receiver_(nullptr) {
    ESP_LOGI(TAG, "IR 학습기 초기화");
}

IRLearner::IRLearner(IRReceiver* ir_receiver) : learning_mode_(false), ir_receiver_(ir_receiver) {
    ESP_LOGI(TAG, "IR 학습기 초기화");

    if (ir_receiver_) {
        ir_receiver_->setIRCodeCallback([this](const std::string& ir_code) {
            this->onIRCodeReceived(ir_code);
        });
    }
}

IRLearner::~IRLearner() {
    stopLearningMode();
}

bool IRLearner::startLearningMode() {
    if (learning_mode_) {
        return false;
    }

    learning_mode_ = true;
    ESP_LOGI(TAG, "IR 학습 모드 시작");
    return true;
}

void IRLearner::stopLearningMode() {
    learning_mode_ = false;
    ESP_LOGI(TAG, "IR 학습 모드 중지");
}

bool IRLearner::isLearningMode() const {
    return learning_mode_;
}

bool IRLearner::learnIRCode(const std::string& appliance_id, const std::string& command_name) {
    return learnIRCode(appliance_id, command_name, "");
}

bool IRLearner::learnIRCode(const std::string& appliance_id, const std::string& command_name,
                           const std::string& description) {
    if (!learning_mode_) {
        ESP_LOGE(TAG, "학습 모드가 활성화되지 않음");
        return false;
    }

    if (!ir_receiver_) {
        ESP_LOGE(TAG, "IR 수신기가 설정되지 않음");
        return false;
    }

    ESP_LOGI(TAG, "IR 코드 학습 시작: %s - %s", appliance_id.c_str(), command_name.c_str());

    current_appliance_id_ = appliance_id;
    current_command_name_ = command_name;

    if (!ir_receiver_->isReceiving()) {
        ir_receiver_->startReceiving();
    }

    return true;
}

std::vector<LearnedCommand> IRLearner::getLearnedCommands(const std::string& appliance_id) const {
    std::lock_guard<std::mutex> lock(commands_mutex_);
    auto it = learned_commands_.find(appliance_id);
    return (it != learned_commands_.end()) ? it->second : std::vector<LearnedCommand>();
}

std::vector<LearnedCommand> IRLearner::getAllLearnedCommands() const {
    std::lock_guard<std::mutex> lock(commands_mutex_);
    std::vector<LearnedCommand> all_commands;

    for (const auto& pair : learned_commands_) {
        all_commands.insert(all_commands.end(), pair.second.begin(), pair.second.end());
    }

    return all_commands;
}

bool IRLearner::deleteLearnedCommand(const std::string& appliance_id, const std::string& command_name) {
    std::lock_guard<std::mutex> lock(commands_mutex_);
    auto it = learned_commands_.find(appliance_id);
    if (it == learned_commands_.end()) {
        return false;
    }

    auto& commands = it->second;
    commands.erase(
        std::remove_if(commands.begin(), commands.end(),
            [&command_name](const LearnedCommand& cmd) {
                return cmd.command_name == command_name;
            }),
        commands.end()
    );

    ESP_LOGI(TAG, "학습된 명령 삭제: %s - %s", appliance_id.c_str(), command_name.c_str());
    return true;
}

bool IRLearner::validateIRCode(const std::string& ir_code) const {
    if (ir_code.empty() || ir_code.length() < 3) {
        return false;
    }

    if (ir_code.substr(0, 2) != "0x") {
        return false;
    }

    for (size_t i = 2; i < ir_code.length(); i++) {
        char c = ir_code[i];
        if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'))) {
            return false;
        }
    }

    return true;
}

std::string IRLearner::detectProtocol(const std::string& ir_code) const {
    if (ir_code.length() == 10) {
        return "NEC";
    } else if (ir_code.length() == 8) {
        return "RC5";
    } else if (ir_code.length() == 6) {
        return "Sony";
    }

    return "Unknown";
}

bool IRLearner::saveLearnedCodes(const std::string& filename) const {
    try {
        std::ofstream file(filename);
        if (!file.is_open()) {
            ESP_LOGE(TAG, "파일 열기 실패: %s", filename.c_str());
            return false;
        }

        file << "{\n";
        file << "  \"learned_commands\": [\n";

        std::lock_guard<std::mutex> lock(commands_mutex_);
        bool first = true;
        for (const auto& pair : learned_commands_) {
            for (const auto& command : pair.second) {
                if (!first) file << ",\n";
                file << "    {\n";
                file << "      \"appliance_id\": \"" << command.appliance_id << "\",\n";
                file << "      \"command_name\": \"" << command.command_name << "\",\n";
                file << "      \"ir_code\": \"" << command.ir_code.code << "\",\n";
                file << "      \"protocol\": \"" << command.ir_code.protocol << "\",\n";
                file << "      \"frequency\": " << command.ir_code.frequency << ",\n";
                file << "      \"bits\": " << command.ir_code.bits << ",\n";
                file << "      \"description\": \"" << command.ir_code.description << "\",\n";
                file << "      \"notes\": \"" << command.notes << "\"\n";
                file << "    }";
                first = false;
            }
        }

        file << "\n  ]\n";
        file << "}\n";

        ESP_LOGI(TAG, "학습된 IR 코드 저장 완료: %s", filename.c_str());
        return true;

    } catch (const std::exception& e) {
        ESP_LOGE(TAG, "IR 코드 저장 실패: %s", e.what());
        return false;
    }
}

bool IRLearner::loadLearnedCodes(const std::string& filename) {
    try {
        std::ifstream file(filename);
        if (!file.is_open()) {
            ESP_LOGE(TAG, "파일 열기 실패: %s", filename.c_str());
            return false;
        }

        DynamicJsonDocument doc(4096);
        DeserializationError error = deserializeJson(doc, file);

        if (error) {
            ESP_LOGE(TAG, "JSON 파싱 오류: %s", error.c_str());
            return false;
        }

        std::lock_guard<std::mutex> lock(commands_mutex_);
        learned_commands_.clear();

        if (doc.containsKey("learned_commands")) {
            JsonArray commands = doc["learned_commands"];
            for (JsonObject cmd : commands) {
                LearnedCommand command;
                command.appliance_id = cmd["appliance_id"].as<std::string>();
                command.command_name = cmd["command_name"].as<std::string>();
                command.ir_code.code = cmd["ir_code"].as<std::string>();
                command.ir_code.protocol = cmd["protocol"].as<std::string>();
                command.ir_code.frequency = cmd["frequency"].as<int>();
                command.ir_code.bits = cmd["bits"].as<int>();
                command.ir_code.description = cmd["description"].as<std::string>();
                command.repeat_count = cmd["repeat_count"].as<int>();
                command.notes = cmd["notes"].as<std::string>();

                command.ir_code.timestamp = std::chrono::steady_clock::now();

                learned_commands_[command.appliance_id].push_back(command);
            }
        }

        ESP_LOGI(TAG, "학습된 IR 코드 로드 완료: %d개 명령어", learned_commands_.size());
        return true;

    } catch (const std::exception& e) {
        ESP_LOGE(TAG, "IR 코드 로드 실패: %s", e.what());
        return false;
    }
}

void IRLearner::setLearningCallback(std::function<void(const IRCode&)> callback) {
    learning_callback_ = callback;
}

void IRLearner::setValidationCallback(std::function<bool(const IRCode&)> callback) {
    validation_callback_ = callback;
}

IRCode IRLearner::analyzeIRCode(const std::string& raw_code) const {
    IRCode code;
    code.code = raw_code;
    code.protocol = detectProtocol(raw_code);
    code.frequency = 38000;
    code.bits = (raw_code.length() - 2) * 4;
    code.timestamp = std::chrono::steady_clock::now();
    return code;
}

bool IRLearner::isDuplicateCode(const IRCode& code) const {
    std::lock_guard<std::mutex> lock(commands_mutex_);

    for (const auto& pair : learned_commands_) {
        for (const auto& command : pair.second) {
            if (command.ir_code.code == code.code) {
                return true;
            }
        }
    }

    return false;
}

std::string IRLearner::generateCodeHash(const IRCode& code) const {
    std::stringstream ss;
    ss << code.code << "_" << code.protocol << "_" << code.frequency;
    return ss.str();
}

void IRLearner::setIRReceiver(IRReceiver* ir_receiver) {
    ir_receiver_ = ir_receiver;

    if (ir_receiver_) {
        ir_receiver_->setIRCodeCallback([this](const std::string& ir_code) {
            this->onIRCodeReceived(ir_code);
        });
        ESP_LOGI(TAG, "IR 수신기 연동 완료");
    }
}

IRReceiver* IRLearner::getIRReceiver() const {
    return ir_receiver_;
}

void IRLearner::onIRCodeReceived(const std::string& ir_code) {
    if (!learning_mode_ || current_appliance_id_.empty() || current_command_name_.empty()) {
        return;
    }

    ESP_LOGI(TAG, "IR 코드 수신됨: %s", ir_code.c_str());

    IRCode analyzed_code = analyzeIRCode(ir_code);
    analyzed_code.description = "학습된 코드: " + current_command_name_;

    if (validation_callback_ && !validation_callback_(analyzed_code)) {
        ESP_LOGE(TAG, "IR 코드 검증 실패: %s", ir_code.c_str());
        return;
    }

    if (isDuplicateCode(analyzed_code)) {
        ESP_LOGW(TAG, "중복된 IR 코드 감지: %s", ir_code.c_str());
        return;
    }

    LearnedCommand command;
    command.appliance_id = current_appliance_id_;
    command.command_name = current_command_name_;
    command.ir_code = analyzed_code;
    command.repeat_count = 1;
    command.notes = "학습됨";

    {
        std::lock_guard<std::mutex> lock(commands_mutex_);
        learned_commands_[current_appliance_id_].push_back(command);
    }

    ESP_LOGI(TAG, "IR 코드 학습 완료: %s -> %s (%s)",
             analyzed_code.code.c_str(), current_appliance_id_.c_str(), current_command_name_.c_str());

    if (learning_callback_) {
        learning_callback_(analyzed_code);
    }

    current_appliance_id_.clear();
    current_command_name_.clear();
}
