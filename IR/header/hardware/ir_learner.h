#ifndef IR_LEARNER_H
#define IR_LEARNER_H

#include <string>
#include <vector>
#include <map>
#include <functional>
#include <atomic>
#include <thread>
#include <memory>
#include <mutex>

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


class IRReceiver;

class IRLearner {
public:
    IRLearner();
    IRLearner(IRReceiver* ir_receiver);
    ~IRLearner();


    bool startLearningMode();
    void stopLearningMode();
    bool isLearningMode() const;


    bool learnIRCode(const std::string& appliance_id, const std::string& command_name);
    bool learnIRCode(const std::string& appliance_id, const std::string& command_name,
                     const std::string& description);


    std::vector<LearnedCommand> getLearnedCommands(const std::string& appliance_id) const;
    std::vector<LearnedCommand> getAllLearnedCommands() const;
    bool deleteLearnedCommand(const std::string& appliance_id, const std::string& command_name);


    bool validateIRCode(const std::string& ir_code) const;
    std::string detectProtocol(const std::string& ir_code) const;


    bool saveLearnedCodes(const std::string& filename) const;
    bool loadLearnedCodes(const std::string& filename);


    void setLearningCallback(std::function<void(const IRCode&)> callback);
    void setValidationCallback(std::function<bool(const IRCode&)> callback);


    void setIRReceiver(IRReceiver* ir_receiver);
    IRReceiver* getIRReceiver() const;


    void onIRCodeReceived(const std::string& ir_code);

private:
    std::atomic<bool> learning_mode_;
    std::map<std::string, std::vector<LearnedCommand>> learned_commands_;
    std::function<void(const IRCode&)> learning_callback_;
    std::function<bool(const IRCode&)> validation_callback_;
    mutable std::mutex commands_mutex_;


    IRReceiver* ir_receiver_;
    std::string current_appliance_id_;
    std::string current_command_name_;


    IRCode analyzeIRCode(const std::string& raw_code) const;
    bool isDuplicateCode(const IRCode& code) const;
    std::string generateCodeHash(const IRCode& code) const;


    std::string detectNECProtocol(const std::string& code) const;
    std::string detectRC5Protocol(const std::string& code) const;
    std::string detectSonyProtocol(const std::string& code) const;
    std::string detectSamsungProtocol(const std::string& code) const;
};

#endif
