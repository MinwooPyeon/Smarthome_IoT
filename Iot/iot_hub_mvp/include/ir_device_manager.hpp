#pragma once
#include <vector>
#include <fstream>
#include <string>

#include <types.hpp>

class IrDeviceManager{
public:
    void loadData();
    void addData(IrSendDevice& device);
    void deleteData(IrSendDevice& device);
private:
    std::string csvPath_ = "IrDevice.csv";
    std::vector<IrSendDevice> devices_;
};