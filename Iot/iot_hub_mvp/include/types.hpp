#pragma once
#include <vector>
#include <string>
#include <chrono>

struct EnvSample{
    long long tsMs; // timestamp
    double t, h; // temperature , humidity
};

struct IrSample{
    std::vector<int> rawUs;
    int gapUs;
};

struct Metrics{
    std::chrono::system_clock::time_point ts{};

    double tAvg = 0;
    double hAvg = 0;
    double tEwma = 0;    // EWMA 온도
    double hEwma = 0;    // EWMA 습도
    double dewPoint = 0; // 이슬점(°C)
    double heatIndex = 0;// 체감온도(°C)
    double absHumidity = 0; // 절대습도 (g/m^3)
    double wbgt = 0;        // WBGT (근사, °C)
    double pmv = 0;         // PMV (-3 ~ +3)
    double ppd = 0;         // PPD (%)
    bool   spike = false;
};

struct IrSendDevice{
    std::string deviceId;
    std::string deviceType;
    float       consumption;
};

struct IrSignalLog {
    std::chrono::system_clock::time_point ts{};
    int64_t tx_id{};
    std::string send_device_id;
    std::string device_type;
    std::string function_label;
    std::vector<std::string> meta_data;
};

struct Dialect{
    char delimiter = ',';
    char quote = '"';
    char escape = '"';
    bool write_header = true;
    bool trim_whitespace =false;
    bool allow_bom = true;
    
};