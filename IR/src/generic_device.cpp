#include "hardware/generic_device.h"
#include "core/platform.h"
#include <fstream>
#include <algorithm>
#include <sstream>
#include <iomanip>

GenericDeviceManager::GenericDeviceManager() {
    initializeDefaultMappings();
    LOG_INFO("범용 기기 관리자 초기화 완료");
}

GenericDeviceManager::~GenericDeviceManager() {
    // 정리 작업
}

void GenericDeviceManager::initializeDefaultMappings() {
    // 기기 타입별 기본 버튼 매핑
    default_buttons_["TV"] = {"power", "volume_up", "volume_down", "channel_up", "channel_down", 
                              "mute", "input", "menu", "back", "ok"};
    default_buttons_["AIR_CONDITIONER"] = {"power", "temp_up", "temp_down", "mode", "fan_speed", 
                                          "swing", "timer", "sleep"};
    default_buttons_["AUDIO"] = {"power", "volume_up", "volume_down", "mute", "play", "pause", 
                                "stop", "next", "prev", "mode"};
    default_buttons_["PROJECTOR"] = {"power", "input", "menu", "ok", "back", "up", "down", 
                                    "left", "right", "zoom"};
    default_buttons_["FAN"] = {"power", "speed_1", "speed_2", "speed_3", "oscillate", "timer"};
    default_buttons_["LIGHT"] = {"power", "brightness_up", "brightness_down", "color_change", 
                                "mode", "timer"};
    
    // 프로토콜별 기기 타입 추정
    protocol_device_types_["NEC"] = {"TV", "AIR_CONDITIONER", "AUDIO", "PROJECTOR"};
    protocol_device_types_["Sony"] = {"TV", "AUDIO", "PROJECTOR"};
    protocol_device_types_["RC5"] = {"TV", "AUDIO", "AIR_CONDITIONER"};
    protocol_device_types_["Samsung"] = {"TV", "AIR_CONDITIONER", "AUDIO"};
    protocol_device_types_["LG"] = {"TV", "AIR_CONDITIONER", "AUDIO"};
    protocol_device_types_["Panasonic"] = {"TV", "AIR_CONDITIONER", "AUDIO"};
}

bool GenericDeviceManager::registerGenericDevice(const std::string& device_id, 
                                                const std::string& device_name, 
                                                const std::string& device_type) {
    std::lock_guard<std::mutex> lock(devices_mutex_);
    
    if (devices_.find(device_id) != devices_.end()) {
        LOG_ERROR("기기가 이미 등록됨: %s", device_id.c_str());
        return false;
    }
    
    auto device = std::make_unique<GenericDevice>();
    device->device_id = device_id;
    device->device_name = device_name;
    device->device_type = device_type;
    device->brand = "Unknown";
    device->model = "Unknown";
    device->is_learned = false;
    device->created_time = std::chrono::steady_clock::now();
    
    devices_[device_id] = std::move(device);
    
    LOG_INFO("범용 기기 등록: %s (%s)", device_name.c_str(), device_type.c_str());
    return true;
}

bool GenericDeviceManager::learnIRCode(const std::string& device_id, const std::string& button_name, 
                                      const std::string& ir_code, const std::string& protocol) {
    std::lock_guard<std::mutex> lock(devices_mutex_);
    
    auto it = devices_.find(device_id);
    if (it == devices_.end()) {
        LOG_ERROR("등록되지 않은 기기: %s", device_id.c_str());
        return false;
    }
    
    GenericDevice* device = it->second.get();
    
    // IR 코드 검증
    if (!validateIRCode(ir_code, protocol)) {
        LOG_ERROR("잘못된 IR 코드: %s", ir_code.c_str());
        return false;
    }
    
    // 기존 버튼 코드 업데이트 또는 새로 추가
    bool found = false;
    for (auto& code : device->ir_codes) {
        if (code.button_name == button_name) {
            code.code = ir_code;
            code.protocol = protocol;
            code.timestamp = std::chrono::steady_clock::now();
            found = true;
            break;
        }
    }
    
    if (!found) {
        GenericIRCode new_code;
        new_code.code = ir_code;
        new_code.protocol = protocol;
        new_code.button_name = button_name;
        new_code.description = "학습된 코드: " + button_name;
        new_code.timestamp = std::chrono::steady_clock::now();
        device->ir_codes.push_back(new_code);
    }
    
    // 버튼 매핑 업데이트
    device->button_mapping[button_name] = ir_code;
    device->is_learned = true;
    
    LOG_INFO("IR 코드 학습 완료: %s - %s (%s)", device_id.c_str(), button_name.c_str(), ir_code.c_str());
    return true;
}

GenericDevice* GenericDeviceManager::getDevice(const std::string& device_id) {
    std::lock_guard<std::mutex> lock(devices_mutex_);
    auto it = devices_.find(device_id);
    return (it != devices_.end()) ? it->second.get() : nullptr;
}

std::vector<GenericDevice*> GenericDeviceManager::getAllDevices() {
    std::lock_guard<std::mutex> lock(devices_mutex_);
    std::vector<GenericDevice*> result;
    
    for (const auto& pair : devices_) {
        result.push_back(pair.second.get());
    }
    
    return result;
}

std::vector<GenericDevice*> GenericDeviceManager::getDevicesByType(const std::string& device_type) {
    std::lock_guard<std::mutex> lock(devices_mutex_);
    std::vector<GenericDevice*> result;
    
    for (const auto& pair : devices_) {
        if (pair.second->device_type == device_type) {
            result.push_back(pair.second.get());
        }
    }
    
    return result;
}

std::string GenericDeviceManager::getIRCode(const std::string& device_id, const std::string& button_name) {
    std::lock_guard<std::mutex> lock(devices_mutex_);
    auto it = devices_.find(device_id);
    if (it == devices_.end()) {
        return "";
    }
    
    GenericDevice* device = it->second.get();
    auto button_it = device->button_mapping.find(button_name);
    return (button_it != device->button_mapping.end()) ? button_it->second : "";
}

std::vector<std::string> GenericDeviceManager::getAvailableButtons(const std::string& device_id) {
    std::lock_guard<std::mutex> lock(devices_mutex_);
    auto it = devices_.find(device_id);
    if (it == devices_.end()) {
        return {};
    }
    
    GenericDevice* device = it->second.get();
    std::vector<std::string> buttons;
    
    for (const auto& pair : device->button_mapping) {
        buttons.push_back(pair.first);
    }
    
    return buttons;
}

bool GenericDeviceManager::removeDevice(const std::string& device_id) {
    std::lock_guard<std::mutex> lock(devices_mutex_);
    auto it = devices_.find(device_id);
    if (it == devices_.end()) {
        return false;
    }
    
    devices_.erase(it);
    LOG_INFO("기기 제거: %s", device_id.c_str());
    return true;
}

bool GenericDeviceManager::updateDeviceInfo(const std::string& device_id, const std::string& field, 
                                           const std::string& value) {
    std::lock_guard<std::mutex> lock(devices_mutex_);
    auto it = devices_.find(device_id);
    if (it == devices_.end()) {
        return false;
    }
    
    GenericDevice* device = it->second.get();
    
    if (field == "device_name") {
        device->device_name = value;
    } else if (field == "brand") {
        device->brand = value;
    } else if (field == "model") {
        device->model = value;
    } else {
        LOG_ERROR("알 수 없는 필드: %s", field.c_str());
        return false;
    }
    
    LOG_INFO("기기 정보 업데이트: %s.%s = %s", device_id.c_str(), field.c_str(), value.c_str());
    return true;
}

std::string GenericDeviceManager::detectDeviceType(const std::string& ir_code, const std::string& protocol) {
    // IR 코드와 프로토콜을 기반으로 기기 타입 추정
    // 실제로는 더 정교한 알고리즘이 필요하지만, 여기서는 간단한 추정
    
    if (protocol == "NEC") {
        // NEC 프로토콜은 주로 TV, 에어컨 등에서 사용
        return "TV"; // 기본값
    } else if (protocol == "Sony") {
        return "AUDIO"; // Sony는 주로 오디오 기기
    } else if (protocol == "RC5") {
        return "TV"; // RC5는 주로 TV에서 사용
    }
    
    return "UNKNOWN";
}

std::string GenericDeviceManager::suggestDeviceName(const std::string& device_type, const std::string& protocol) {
    std::stringstream ss;
    ss << protocol << "_" << device_type << "_" << std::time(nullptr);
    return ss.str();
}

bool GenericDeviceManager::saveDevices(const std::string& filename) {
    try {
        std::ofstream file(filename);
        if (!file.is_open()) {
            LOG_ERROR("파일 열기 실패: %s", filename.c_str());
            return false;
        }
        
        file << "{\n";
        file << "  \"generic_devices\": [\n";
        
        std::lock_guard<std::mutex> lock(devices_mutex_);
        bool first = true;
        for (const auto& pair : devices_) {
            const GenericDevice* device = pair.second.get();
            
            if (!first) file << ",\n";
            file << "    {\n";
            file << "      \"device_id\": \"" << device->device_id << "\",\n";
            file << "      \"device_name\": \"" << device->device_name << "\",\n";
            file << "      \"device_type\": \"" << device->device_type << "\",\n";
            file << "      \"brand\": \"" << device->brand << "\",\n";
            file << "      \"model\": \"" << device->model << "\",\n";
            file << "      \"is_learned\": " << (device->is_learned ? "true" : "false") << ",\n";
            file << "      \"ir_codes\": [\n";
            
            bool first_code = true;
            for (const auto& code : device->ir_codes) {
                if (!first_code) file << ",\n";
                file << "        {\n";
                file << "          \"code\": \"" << code.code << "\",\n";
                file << "          \"protocol\": \"" << code.protocol << "\",\n";
                file << "          \"button_name\": \"" << code.button_name << "\",\n";
                file << "          \"description\": \"" << code.description << "\"\n";
                file << "        }";
                first_code = false;
            }
            
            file << "\n      ]\n";
            file << "    }";
            first = false;
        }
        
        file << "\n  ]\n";
        file << "}\n";
        
        LOG_INFO("범용 기기 저장 완료: %s", filename.c_str());
        return true;
        
    } catch (const std::exception& e) {
        LOG_ERROR("기기 저장 실패: %s", e.what());
        return false;
    }
}

bool GenericDeviceManager::loadDevices(const std::string& filename) {
    // TODO: JSON 파싱으로 기기 정보 로드
    LOG_INFO("범용 기기 로드: %s", filename.c_str());
    return true;
}

std::string GenericDeviceManager::generateDeviceId(const std::string& device_name, const std::string& device_type) {
    std::stringstream ss;
    ss << device_type << "_" << device_name << "_" << std::time(nullptr);
    return ss.str();
}

bool GenericDeviceManager::validateIRCode(const std::string& ir_code, const std::string& protocol) {
    if (ir_code.empty() || ir_code.length() < 3) {
        return false;
    }
    
    // 16진수 형식 검증
    if (ir_code.substr(0, 2) != "0x") {
        return false;
    }
    
    // 16진수 문자 검증
    for (size_t i = 2; i < ir_code.length(); i++) {
        char c = ir_code[i];
        if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'))) {
            return false;
        }
    }
    
    return true;
}
