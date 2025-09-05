#ifndef GENERIC_DEVICE_H
#define GENERIC_DEVICE_H

#include <string>
#include <vector>
#include <map>
#include <memory>

struct GenericIRCode {
    std::string code;
    std::string protocol;
    int frequency;
    int bits;
    std::string description;
    std::string button_name;
    std::chrono::steady_clock::time_point timestamp;
};

struct GenericDevice {
    std::string device_id;
    std::string device_name;
    std::string device_type;
    std::string brand;
    std::string model;
    std::vector<GenericIRCode> ir_codes;
    std::map<std::string, std::string> button_mapping;
    bool is_learned;
    std::chrono::steady_clock::time_point created_time;
};

class GenericDeviceManager {
public:
    GenericDeviceManager();
    ~GenericDeviceManager();
    
    // 범용 기기 등록
    bool registerGenericDevice(const std::string& device_id, const std::string& device_name, 
                              const std::string& device_type);
    
    // IR 코드 학습
    bool learnIRCode(const std::string& device_id, const std::string& button_name, 
                    const std::string& ir_code, const std::string& protocol);
    
    // 기기 정보 조회
    GenericDevice* getDevice(const std::string& device_id);
    std::vector<GenericDevice*> getAllDevices();
    std::vector<GenericDevice*> getDevicesByType(const std::string& device_type);
    
    // IR 코드 조회
    std::string getIRCode(const std::string& device_id, const std::string& button_name);
    std::vector<std::string> getAvailableButtons(const std::string& device_id);
    
    // 기기 관리
    bool removeDevice(const std::string& device_id);
    bool updateDeviceInfo(const std::string& device_id, const std::string& field, const std::string& value);
    
    // 저장/로드
    bool saveDevices(const std::string& filename);
    bool loadDevices(const std::string& filename);
    
    // 자동 기기 감지
    std::string detectDeviceType(const std::string& ir_code, const std::string& protocol);
    std::string suggestDeviceName(const std::string& device_type, const std::string& protocol);

private:
    std::map<std::string, std::unique_ptr<GenericDevice>> devices_;
    mutable std::mutex devices_mutex_;
    
    // 기기 타입별 기본 버튼 매핑
    std::map<std::string, std::vector<std::string>> default_buttons_;
    
    // 프로토콜별 기기 타입 추정
    std::map<std::string, std::vector<std::string>> protocol_device_types_;
    
    void initializeDefaultMappings();
    std::string generateDeviceId(const std::string& device_name, const std::string& device_type);
    bool validateIRCode(const std::string& ir_code, const std::string& protocol);
};

#endif // GENERIC_DEVICE_H
