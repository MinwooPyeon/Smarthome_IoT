#include "ir_device_manager.hpp"
#include <iostream>
#include <sstream>

void IrDeviceManager::loadData(){
    std::ifstream infile(csvPath_);
    if(!infile.is_open()){
        std::ofstream outfile(csvPath_);
        if (!outfile.is_open()) {
            std::cerr << csvPath_ <<" file create failed" << std::endl;
            return;
        }
        outfile << "deviceId,deviceType,consumption\n"; //header
        outfile.close();

        std::cout << "new " << csvPath_ <<" created\n"; 
        return;
    }

    std::string line;
    std::getline(infile, line);
    while(std::getline(infile, line)){
        std::stringstream ss(line);
        std::string cell;

        IrSendDevice device;

        std::getline(ss, cell, ',');
        device.deviceId = cell;
        std::getline(ss, cell, ',');
        device.deviceType = cell;
        std::getline(ss, cell, ',');
        device.consumption = stof(cell);

        devices_.push_back(device);
    }
}

void IrDeviceManager::addData(IrSendDevice& device) {
    std::ofstream outfile(csvPath_, std::ios::app);
    if (!outfile.is_open()) {
        std::cerr << "파일 열기 실패: " << csvPath_ << std::endl;
        return;
    }

    outfile << device.deviceId << "," 
            << device.deviceType << "," 
            << device.consumption << "\n";

    outfile.close();

    devices_.push_back(device);
}


void IrDeviceManager::deleteData(IrSendDevice& device){
    auto it = std::find(devices_.begin(), devices_.end(), device);
    if(it!= devices_.end()){
        devices_.erase(it);
    }
    std::ofstream outfile("output.csv", std::ios::trunc);

    outfile << "deviceId,deviceType,consumption\n"; //header
    for(int i =0;i<devices_.size();i++){
        IrSendDevice device = devices_[i];
        outfile << device.deviceId << ',' << device.deviceType<< ',' << device.consumption << '\n';
    }
    outfile.close();
}