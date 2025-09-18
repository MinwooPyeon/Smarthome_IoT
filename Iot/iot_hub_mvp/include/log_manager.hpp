#pragma once
#include <fstream>
#include <string>
#include <nlohmann/json.hpp>
#include "types.hpp"

class LogManager{
public:
    void addLog(Log& log);
private:
    std::string csvPath_ = "log.csv";
};