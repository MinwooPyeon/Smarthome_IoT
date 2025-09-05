#ifndef IR_PROTOCOL_DETECTOR_H
#define IR_PROTOCOL_DETECTOR_H

#include <string>
#include <vector>
#include <map>
#include <chrono>

struct IRTiming {
    std::vector<int> timings; // 마이크로초 단위
    std::vector<bool> levels; // HIGH/LOW
    std::chrono::steady_clock::time_point timestamp;
};

struct ProtocolInfo {
    std::string name;
    std::string description;
    int carrier_frequency;
    int header_mark;
    int header_space;
    int bit_mark;
    int zero_space;
    int one_space;
    int repeat_mark;
    int repeat_space;
    int tolerance_percent;
    double confidence;
};

class IRProtocolDetector {
public:
    IRProtocolDetector();
    ~IRProtocolDetector();
    
    // 프로토콜 감지
    ProtocolInfo detectProtocol(const IRTiming& timing) const;
    std::vector<ProtocolInfo> detectAllPossibleProtocols(const IRTiming& timing) const;
    
    // IR 신호 분석
    IRTiming captureIRSignal(int gpio_pin, int timeout_ms = 5000) const;
    std::string decodeIRSignal(const IRTiming& timing, const ProtocolInfo& protocol) const;
    
    // 프로토콜 검증
    bool validateProtocol(const IRTiming& timing, const ProtocolInfo& protocol) const;
    double calculateConfidence(const IRTiming& timing, const ProtocolInfo& protocol) const;
    
    // 지원 프로토콜 목록
    std::vector<ProtocolInfo> getSupportedProtocols() const;
    bool isProtocolSupported(const std::string& protocol_name) const;
    
    // 프로토콜 정보
    ProtocolInfo getProtocolInfo(const std::string& protocol_name) const;
    std::string getProtocolDescription(const std::string& protocol_name) const;

private:
    std::map<std::string, ProtocolInfo> supported_protocols_;
    
    // 프로토콜 초기화
    void initializeSupportedProtocols();
    
    // 타이밍 분석
    bool isWithinTolerance(int measured, int expected, int tolerance_percent) const;
    std::vector<int> extractBitTimings(const IRTiming& timing) const;
    
    // 프로토콜별 디코딩
    std::string decodeNEC(const IRTiming& timing) const;
    std::string decodeRC5(const IRTiming& timing) const;
    std::string decodeSony(const IRTiming& timing) const;
    std::string decodeSamsung(const IRTiming& timing) const;
    std::string decodeLG(const IRTiming& timing) const;
    std::string decodePanasonic(const IRTiming& timing) const;
    
    // 유틸리티 함수
    std::string timingsToHex(const std::vector<int>& timings) const;
    std::vector<int> hexToTimings(const std::string& hex) const;
    int calculateCarrierFrequency(const IRTiming& timing) const;
};

#endif // IR_PROTOCOL_DETECTOR_H
