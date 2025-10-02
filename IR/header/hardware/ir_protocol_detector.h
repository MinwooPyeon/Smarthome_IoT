#ifndef IR_PROTOCOL_DETECTOR_H
#define IR_PROTOCOL_DETECTOR_H

#include <string>
#include <vector>
#include <map>
#include <chrono>

struct IRTiming {
    std::vector<int> timings;
    std::vector<bool> levels;
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

    ProtocolInfo detectProtocol(const IRTiming& timing) const;
    std::vector<ProtocolInfo> detectAllPossibleProtocols(const IRTiming& timing) const;

    IRTiming captureIRSignal(int gpio_pin, int timeout_ms = 5000) const;
    std::string decodeIRSignal(const IRTiming& timing, const ProtocolInfo& protocol) const;

    bool validateProtocol(const IRTiming& timing, const ProtocolInfo& protocol) const;
    double calculateConfidence(const IRTiming& timing, const ProtocolInfo& protocol) const;

    std::vector<ProtocolInfo> getSupportedProtocols() const;
    bool isProtocolSupported(const std::string& protocol_name) const;

    ProtocolInfo getProtocolInfo(const std::string& protocol_name) const;
    std::string getProtocolDescription(const std::string& protocol_name) const;

private:
    std::map<std::string, ProtocolInfo> supported_protocols_;

    void initializeSupportedProtocols();

    bool isWithinTolerance(int measured, int expected, int tolerance_percent) const;
    std::vector<int> extractBitTimings(const IRTiming& timing) const;

    std::string decodeNEC(const IRTiming& timing) const;
    std::string decodeRC5(const IRTiming& timing) const;
    std::string decodeSony(const IRTiming& timing) const;
    std::string decodeSamsung(const IRTiming& timing) const;
    std::string decodeLG(const IRTiming& timing) const;
    std::string decodePanasonic(const IRTiming& timing) const;

    std::string timingsToHex(const std::vector<int>& timings) const;
    std::vector<int> hexToTimings(const std::string& hex) const;
    int calculateCarrierFrequency(const IRTiming& timing) const;
};

#endif
