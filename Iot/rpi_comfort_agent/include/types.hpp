#pragma once
#include <string>

struct EnvRow {
    long ts{};                 // epoch seconds
    double tAvg{}, hAvg{};
    double tEwma{}, hEwma{};
};

struct LogRow {
    long ts{};
    std::string deviceId;
    std::string device_type;
    std::string meta_data;     // "k=v;k2=v2" 등
};

struct Session {
    long event_ts{};
    std::string device_type;
    std::string function;
    std::string meta_data;
    int outcome{};             // 1=만족, 0=불만
};
