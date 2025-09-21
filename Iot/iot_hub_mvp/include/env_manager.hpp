#pragma once
#include <string>

#include <types.hpp>

class EnvManager{
public:
    void addData(EnvSample& device);
private:
    std::string csvPath_ = "envData.csv";
};