#include "hardware/ir_learner.h"
#include "core/platform.h"
#include <fstream>
#include <algorithm>
#include <sstream>

IRLearner::IRLearner() : learning_mode_(false) {
    LOG_INFO("IR 학습기 초기화 완료");
}

IRLearner::~IRLearner() {
    stopLearningMode();
}

bool IRLearner::startLearningMode() {
    if (learning_mode_) {
        return false;
    }
    
    learning_mode_ = true;
    LOG_INFO("IR 학습 모드 시작");
    return true;
}

void IRLearner::stopLearningMode() {
    learning_mode_ = false;
    LOG_INFO("IR 학습 모드 중지");
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
        LOG_ERROR("학습 모드가 활성화되지 않음");
        return false;
    }
    
    LOG_INFO("IR 코드 학습 시작: %s - %s", appliance_id.c_str(), command_name.c_str());
    LOG_INFO("리모컨 버튼을 눌러주세요...");
    
    // 실제 IR 신호 수신 대기 (시뮬레이션)
    // TODO: 실제 IR 수신기와 연동
    
    // 시뮬레이션용 코드
    IRCode simulated_code;
    simulated_code.code = "0x" + std::to_string(rand() % 0xFFFFFFFF);
    simulated_code.protocol = "NEC";
    simulated_code.frequency = 38000;
    simulated_code.bits = 32;
    simulated_code.description = description;
    simulated_code.timestamp = std::chrono::steady_clock::now();
    
    // 코드 검증
    if (validation_callback_ && !validation_callback_(simulated_code)) {
        LOG_ERROR("IR 코드 검증 실패");
        return false;
    }
    
    // 중복 검사
    if (isDuplicateCode(simulated_code)) {
        LOG_WARNING("중복된 IR 코드 감지");
        return false;
    }
    
    // 학습된 명령 저장
    LearnedCommand command;
    command.appliance_id = appliance_id;
    command.command_name = command_name;
    command.ir_code = simulated_code;
    command.repeat_count = 1;
    command.notes = description;
    
    std::lock_guard<std::mutex> lock(commands_mutex_);
    learned_commands_[appliance_id].push_back(command);
    
    LOG_INFO("IR 코드 학습 완료: %s", simulated_code.code.c_str());
    
    // 콜백 호출
    if (learning_callback_) {
        learning_callback_(simulated_code);
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
    
    LOG_INFO("학습된 명령 삭제: %s - %s", appliance_id.c_str(), command_name.c_str());
    return true;
}

bool IRLearner::validateIRCode(const std::string& ir_code) const {
    if (ir_code.empty() || ir_code.length() < 3) {
        return false;
    }
    
    // 16진수 형식 검증
    if (ir_code.substr(0, 2) != "0x") {
        return false;
    }
    
    // 16진수 문자 검증
    for (size_t i = 2; i < ir_code.length(); i++) {
        char c = ir_code[i];
        if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'))) {
            return false;
        }
    }
    
    return true;
}

std::string IRLearner::detectProtocol(const std::string& ir_code) const {
    // 간단한 프로토콜 감지 로직
    if (ir_code.length() == 10) { // 32비트
        return "NEC";
    } else if (ir_code.length() == 8) { // 24비트
        return "RC5";
    } else if (ir_code.length() == 6) { // 12비트
        return "Sony";
    }
    
    return "Unknown";
}

bool IRLearner::saveLearnedCodes(const std::string& filename) const {
    try {
        std::ofstream file(filename);
        if (!file.is_open()) {
            LOG_ERROR("파일 열기 실패: %s", filename.c_str());
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
        
        LOG_INFO("학습된 IR 코드 저장 완료: %s", filename.c_str());
        return true;
        
    } catch (const std::exception& e) {
        LOG_ERROR("IR 코드 저장 실패: %s", e.what());
        return false;
    }
}

bool IRLearner::loadLearnedCodes(const std::string& filename) {
    // TODO: JSON 파싱으로 학습된 코드 로드
    LOG_INFO("학습된 IR 코드 로드: %s", filename.c_str());
    return true;
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
    code.frequency = 38000; // 기본값
    code.bits = (raw_code.length() - 2) * 4; // 16진수 문자당 4비트
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
    // 간단한 해시 생성
    std::stringstream ss;
    ss << code.code << "_" << code.protocol << "_" << code.frequency;
    return ss.str();
}
