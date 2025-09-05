#ifndef IRSEND_H
#define IRSEND_H

#include <string>
#include <vector>
#include <chrono>
#include <mutex>
#include <atomic>

enum class IRSendResult {
    SUCCESS,
    DEVICE_NOT_FOUND,
    INVALID_CODE,
    TRANSMISSION_FAILED,
    TIMEOUT
};

struct IRSendStatus {
    IRSendResult result;
    std::string message;
    double duration_ms;
    
    IRSendStatus(IRSendResult r, const std::string& msg, double dur = 0.0)
        : result(r), message(msg), duration_ms(dur) {}
};

class IRCodeStore; // 전방 선언

class IRSend {
public:
    IRSend();
    ~IRSend();
    IRSend(IRSend&& other) noexcept;
    IRSend& operator=(IRSend&& other) noexcept;
    
    bool initialize();
    void cleanup();
    
    IRSendStatus sendControlSignal(const std::string& control_signal);
    IRSendStatus sendIRCode(const std::string& ir_code);
    std::vector<IRSendStatus> sendControlSignals(const std::vector<std::string>& control_signals, int delay_ms = 100);
    
    void setCodeStore(IRCodeStore* code_store);
    void setDebugMode(bool enabled);
    std::string getLastError() const;
    
    struct Statistics {
        int total_sent = 0;
        int successful_sends = 0;
        int failed_sends = 0;
        double average_duration_ms = 0.0;
        std::chrono::steady_clock::time_point last_send_time;
    };
    Statistics getStatistics() const;

private:
    std::atomic<bool> initialized_;
    std::atomic<bool> debug_mode_;
    IRCodeStore* code_store_;
    Statistics stats_;
    std::string last_error_;
    mutable std::mutex mutex_;
    mutable std::mutex stats_mutex_;
    
#ifdef PLATFORM_ESP32
    class IRsend* irsend_;
#elif defined(PLATFORM_LINUX)
    int lirc_fd_;
    struct lirc_config* config_;
#endif
    
    bool checkDevicePermissions();
    bool validateControlSignal(const std::string& control_signal);
    std::string convertControlSignalToIRCode(const std::string& control_signal);
    void updateStatistics(const IRSendStatus& status);
    void setLastError(const std::string& error);
};

#endif // IRSEND_H
