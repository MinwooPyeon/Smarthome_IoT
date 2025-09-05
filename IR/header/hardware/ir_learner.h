#ifndef IR_LEARNER_H
#define IR_LEARNER_H

#include <string>
#include <vector>
#include <map>
#include <functional>
#include <atomic>
#include <thread>
#include <memory>

struct IRCode {
    std::string code;
    std::string protocol;
    int frequency;
    int bits;
    std::string description;
    std::chrono::steady_clock::time_point timestamp;
};

struct LearnedCommand {
    std::string appliance_id;
    std::string command_name;
    IRCode ir_code;
    int repeat_count;
    std::string notes;
};

// 전방 선언
class IRReceiver;

class IRLearner {
public:
    IRLearner();
    IRLearner(IRReceiver* ir_receiver); // IR 수신기와 연동
    ~IRLearner();
    
    // 학습 모드 시작/중지
    bool startLearningMode();
    void stopLearningMode();
    bool isLearningMode() const;
    
    // IR 코드 학습
    bool learnIRCode(const std::string& appliance_id, const std::string& command_name);
    bool learnIRCode(const std::string& appliance_id, const std::string& command_name, 
                     const std::string& description);
    
    // 학습된 코드 관리
    std::vector<LearnedCommand> getLearnedCommands(const std::string& appliance_id) const;
    std::vector<LearnedCommand> getAllLearnedCommands() const;
    bool deleteLearnedCommand(const std::string& appliance_id, const std::string& command_name);
    
    // 코드 검증
    bool validateIRCode(const std::string& ir_code) const;
    std::string detectProtocol(const std::string& ir_code) const;
    
    // 저장/로드
    bool saveLearnedCodes(const std::string& filename) const;
    bool loadLearnedCodes(const std::string& filename);
    
    // 콜백 설정
    void setLearningCallback(std::function<void(const IRCode&)> callback);
    void setValidationCallback(std::function<bool(const IRCode&)> callback);
    
    // IR 수신기 설정
    void setIRReceiver(IRReceiver* ir_receiver);
    IRReceiver* getIRReceiver() const;

private:
    std::atomic<bool> learning_mode_;
    std::map<std::string, std::vector<LearnedCommand>> learned_commands_;
    std::function<void(const IRCode&)> learning_callback_;
    std::function<bool(const IRCode&)> validation_callback_;
    mutable std::mutex commands_mutex_;
    
    // IR 수신기 연동
    IRReceiver* ir_receiver_;
    std::string current_appliance_id_;
    std::string current_command_name_;
    
    // IR 코드 분석
    IRCode analyzeIRCode(const std::string& raw_code) const;
    bool isDuplicateCode(const IRCode& code) const;
    std::string generateCodeHash(const IRCode& code) const;
    
    // 프로토콜 감지
    std::string detectNECProtocol(const std::string& code) const;
    std::string detectRC5Protocol(const std::string& code) const;
    std::string detectSonyProtocol(const std::string& code) const;
    std::string detectSamsungProtocol(const std::string& code) const;
};

#endif 
