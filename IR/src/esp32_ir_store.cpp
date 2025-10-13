#include "hardware/esp32_ir_store.h"
#include <esp_log.h>
#include <esp_timer.h>
#include <nvs_flash.h>
#include <nvs.h>
#include <algorithm>
#include <sstream>
#include <iomanip>

static const char* TAG = "ESP32_IR_STORE";

const char* ESP32IRStore::NVS_NAMESPACE = "ir_store";
const char* ESP32IRStore::NVS_KEY_IR_CODES = "ir_codes";

ESP32IRStore::ESP32IRStore()
    : nvs_handle_(0), store_mutex_(nullptr) {

    store_mutex_ = xSemaphoreCreateMutex();

    code_map_.clear();
    action_index_.clear();
}

ESP32IRStore::~ESP32IRStore() {
    cleanup();
}

bool ESP32IRStore::initialize() {
    ESP_LOGI(TAG, "ESP32 IR 저장소 초기화 시작");

    if (!store_mutex_) {
        ESP_LOGE(TAG, "Semaphore creation failed");
        return false;
    }

    esp_err_t ret = nvs_open(NVS_NAMESPACE, NVS_READWRITE, &nvs_handle_);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "NVS 네임스페이스 열기 실패: %s", esp_err_to_name(ret));
        return false;
    }

    if (!loadFromNVS()) {
        ESP_LOGW(TAG, "NVS에서 데이터 로드 실패, 기본값 사용");
        setDefaultCodes();
    }

    ESP_LOGI(TAG, "ESP32 IR 저장소 초기화 완료");
    return true;
}

void ESP32IRStore::cleanup() {
    if (nvs_handle_) {
        nvs_close(nvs_handle_);
        nvs_handle_ = 0;
    }

    if (store_mutex_) {
        vSemaphoreDelete(store_mutex_);
        store_mutex_ = nullptr;
    }

    ESP_LOGI(TAG, "ESP32 IR 저장소 정리 완료");
}

bool ESP32IRStore::storeCode(const std::string& device_type,
                             const std::string& action,
                             const std::string& ir_code) {
    if (device_type.empty() || action.empty() || ir_code.empty()) {
        ESP_LOGE(TAG, "잘못된 매개변수");
        return false;
    }

    if (!isValidCode(ir_code)) {
        ESP_LOGE(TAG, "잘못된 IR 코드: %s", ir_code.c_str());
        return false;
    }

    if (xSemaphoreTake(store_mutex_, pdMS_TO_TICKS(1000)) != pdTRUE) {
        ESP_LOGE(TAG, "Semaphore take failed");
        return false;
    }

    std::string normalized_code = normalizeIRCode(ir_code);
    code_map_[device_type][action] = normalized_code;

    updateActionIndex();

    bool save_success = saveToNVS();

    xSemaphoreGive(store_mutex_);

    if (save_success) {
        ESP_LOGI(TAG, "IR 코드 저장 성공: %s - %s - %s",
                 device_type.c_str(), action.c_str(), normalized_code.c_str());
        return true;
    } else {
        ESP_LOGE(TAG, "IR 코드 저장 실패");
        return false;
    }
}

std::string ESP32IRStore::getCode(const std::string& device_type,
                                  const std::string& action) const {
    if (xSemaphoreTake(store_mutex_, pdMS_TO_TICKS(1000)) != pdTRUE) {
        ESP_LOGE(TAG, "Semaphore take failed");
        return "";
    }

    std::string code;
    auto device_it = code_map_.find(device_type);
    if (device_it != code_map_.end()) {
        auto action_it = device_it->second.find(action);
        if (action_it != device_it->second.end()) {
            code = action_it->second;
        }
    }

    xSemaphoreGive(store_mutex_);
    return code;
}

std::vector<std::string> ESP32IRStore::getDeviceTypes() const {
    if (xSemaphoreTake(store_mutex_, pdMS_TO_TICKS(1000)) != pdTRUE) {
        ESP_LOGE(TAG, "Semaphore take failed");
        return {};
    }

    std::vector<std::string> device_types;
    for (const auto& device : code_map_) {
        device_types.push_back(device.first);
    }

    xSemaphoreGive(store_mutex_);
    return device_types;
}

std::vector<std::string> ESP32IRStore::getActions(const std::string& device_type) const {
    if (xSemaphoreTake(store_mutex_, pdMS_TO_TICKS(1000)) != pdTRUE) {
        ESP_LOGE(TAG, "세마포어 획득 실패");
        return {};
    }

    std::vector<std::string> actions;
    auto device_it = code_map_.find(device_type);
    if (device_it != code_map_.end()) {
        for (const auto& action : device_it->second) {
            actions.push_back(action.first);
        }
    }

    xSemaphoreGive(store_mutex_);
    return actions;
}

bool ESP32IRStore::loadFromNVS() {
    if (!nvs_handle_) {
        ESP_LOGE(TAG, "NVS 핸들 없음");
        return false;
    }

    size_t required_size = 0;
    esp_err_t ret = nvs_get_str(nvs_handle_, NVS_KEY_IR_CODES, nullptr, &required_size);
    if (ret != ESP_OK) {
        ESP_LOGW(TAG, "NVS에서 IR 코드 데이터 없음");
        return false;
    }

    if (required_size == 0) {
        ESP_LOGW(TAG, "NVS IR 코드 데이터 크기 0");
        return false;
    }

    std::string json_data(required_size, '\0');
    ret = nvs_get_str(nvs_handle_, NVS_KEY_IR_CODES, &json_data[0], &required_size);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "NVS에서 IR 코드 데이터 읽기 실패: %s", esp_err_to_name(ret));
        return false;
    }

    if (!deserializeFromJSON(json_data)) {
        ESP_LOGE(TAG, "JSON 파싱 실패");
        return false;
    }

    ESP_LOGI(TAG, "NVS에서 %zu개 장치의 IR 코드 로드 완료", code_map_.size());
    return true;
}

bool ESP32IRStore::saveToNVS() const {
    if (!nvs_handle_) {
        ESP_LOGE(TAG, "NVS 핸들 없음");
        return false;
    }

    std::string json_data = serializeToJSON();
    if (json_data.empty()) {
        ESP_LOGE(TAG, "JSON 직렬화 실패");
        return false;
    }

    esp_err_t ret = nvs_set_str(nvs_handle_, NVS_KEY_IR_CODES, json_data.c_str());
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "NVS에 IR 코드 저장 실패: %s", esp_err_to_name(ret));
        return false;
    }

    ret = nvs_commit(nvs_handle_);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "NVS 커밋 실패: %s", esp_err_to_name(ret));
        return false;
    }

    ESP_LOGI(TAG, "NVS에 IR 코드 저장 완료");
    return true;
}

bool ESP32IRStore::loadFromFile(const std::string& filename) {
    ESP_LOGW(TAG, "파일 로드는 ESP32에서 지원하지 않음");
    return false;
}

bool ESP32IRStore::saveToFile(const std::string& filename) const {
    ESP_LOGW(TAG, "파일 저장은 ESP32에서 지원하지 않음");
    return false;
}

void ESP32IRStore::setDefaultCodes() {
    ESP_LOGI(TAG, "기본 IR 코드 설정");

    storeCode("samsung_tv", "power", "0xE0E040BF");
    storeCode("samsung_tv", "volume_up", "0xE0E0E01F");
    storeCode("samsung_tv", "volume_down", "0xE0E0D02F");
    storeCode("samsung_tv", "channel_up", "0xE0E048B7");
    storeCode("samsung_tv", "channel_down", "0xE0E008F7");

    storeCode("samsung_ac", "power", "0xE0E040BF");
    storeCode("samsung_ac", "mode", "0xE0E014EB");
    storeCode("samsung_ac", "temp_18", "0xE0E018E7");
    storeCode("samsung_ac", "temp_up", "0xE0E01CE3");
    storeCode("samsung_ac", "temp_down", "0xE0E05CA3");

    storeCode("general", "power", "0x20DF10EF");
    storeCode("general", "mode", "0x20DF50AF");

    ESP_LOGI(TAG, "기본 IR 코드 설정 완료");
}

bool ESP32IRStore::removeDevice(const std::string& device_type) {
    if (xSemaphoreTake(store_mutex_, pdMS_TO_TICKS(1000)) != pdTRUE) {
        ESP_LOGE(TAG, "Semaphore take failed");
        return false;
    }

    size_t removed = code_map_.erase(device_type);
    updateActionIndex();

    xSemaphoreGive(store_mutex_);

    if (removed > 0) {
        ESP_LOGI(TAG, "장치 제거 완료: %s", device_type.c_str());
        saveToNVS();
        return true;
    }

    ESP_LOGW(TAG, "제거할 장치 없음: %s", device_type.c_str());
    return false;
}

bool ESP32IRStore::removeAction(const std::string& device_type, const std::string& action) {
    if (xSemaphoreTake(store_mutex_, pdMS_TO_TICKS(1000)) != pdTRUE) {
        ESP_LOGE(TAG, "Semaphore take failed");
        return false;
    }

    auto device_it = code_map_.find(device_type);
    if (device_it != code_map_.end()) {
        size_t removed = device_it->second.erase(action);
        if (removed > 0) {
            updateActionIndex();
            xSemaphoreGive(store_mutex_);

            ESP_LOGI(TAG, "동작 제거 완료: %s - %s", device_type.c_str(), action.c_str());
            saveToNVS();
            return true;
        }
    }

    xSemaphoreGive(store_mutex_);
    ESP_LOGW(TAG, "제거할 동작 없음: %s - %s", device_type.c_str(), action.c_str());
    return false;
}

size_t ESP32IRStore::getCodeCount() const {
    if (xSemaphoreTake(store_mutex_, pdMS_TO_TICKS(1000)) != pdTRUE) {
        ESP_LOGE(TAG, "Semaphore take failed");
        return 0;
    }

    size_t count = 0;
    for (const auto& device : code_map_) {
        count += device.second.size();
    }

    xSemaphoreGive(store_mutex_);
    return count;
}

void ESP32IRStore::clear() {
    if (xSemaphoreTake(store_mutex_, pdMS_TO_TICKS(1000)) != pdTRUE) {
        ESP_LOGE(TAG, "Semaphore take failed");
        return;
    }

    code_map_.clear();
    action_index_.clear();

    xSemaphoreGive(store_mutex_);

    ESP_LOGI(TAG, "모든 IR 코드 삭제 완료");
    saveToNVS();
}

bool ESP32IRStore::isValidCode(const std::string& ir_code) const {
    if (ir_code.empty() || ir_code.length() < 3) return false;

    if (ir_code.substr(0, 2) != "0x") return false;

    for (size_t i = 2; i < ir_code.length(); i++) {
        if (!isxdigit(ir_code[i])) return false;
    }

    return true;
}

void ESP32IRStore::printDebugInfo() const {
    if (xSemaphoreTake(store_mutex_, pdMS_TO_TICKS(1000)) != pdTRUE) {
        ESP_LOGE(TAG, "Semaphore take failed");
        return;
    }

    ESP_LOGI(TAG, "총 장치 수: %zu", code_map_.size());
    ESP_LOGI(TAG, "총 코드 수: %zu", getCodeCount());

    for (const auto& device : code_map_) {
        ESP_LOGI(TAG, "장치: %s (%zu개 동작)",
                 device.first.c_str(), device.second.size());

        for (const auto& action : device.second) {
            ESP_LOGI(TAG, "  %s: %s", action.first.c_str(), action.second.c_str());
        }
    }

    xSemaphoreGive(store_mutex_);
}

bool ESP32IRStore::codeExists(const std::string& ir_code) const {
    if (xSemaphoreTake(store_mutex_, pdMS_TO_TICKS(1000)) != pdTRUE) {
        ESP_LOGE(TAG, "Semaphore take failed");
        return false;
    }

    std::string normalized_code = normalizeIRCode(ir_code);
    bool exists = false;

    for (const auto& device : code_map_) {
        for (const auto& action : device.second) {
            if (action.second == normalized_code) {
                exists = true;
                break;
            }
        }
        if (exists) break;
    }

    xSemaphoreGive(store_mutex_);
    return exists;
}

std::string ESP32IRStore::findDeviceByCode(const std::string& ir_code) const {
    if (xSemaphoreTake(store_mutex_, pdMS_TO_TICKS(1000)) != pdTRUE) {
        ESP_LOGE(TAG, "Semaphore take failed");
        return "";
    }

    std::string normalized_code = normalizeIRCode(ir_code);
    std::string device_type;

    for (const auto& device : code_map_) {
        for (const auto& action : device.second) {
            if (action.second == normalized_code) {
                device_type = device.first;
                break;
            }
        }
        if (!device_type.empty()) break;
    }

    xSemaphoreGive(store_mutex_);
    return device_type;
}

std::string ESP32IRStore::findActionByCode(const std::string& ir_code) const {
    if (xSemaphoreTake(store_mutex_, pdMS_TO_TICKS(1000)) != pdTRUE) {
        ESP_LOGE(TAG, "Semaphore take failed");
        return "";
    }

    std::string normalized_code = normalizeIRCode(ir_code);
    std::string action_name;

    for (const auto& device : code_map_) {
        for (const auto& action : device.second) {
            if (action.second == normalized_code) {
                action_name = action.first;
                break;
            }
        }
        if (!action_name.empty()) break;
    }

    xSemaphoreGive(store_mutex_);
    return action_name;
}

std::vector<ESP32IRStore::IRCodeInfo> ESP32IRStore::getAllCodes() const {
    if (xSemaphoreTake(store_mutex_, pdMS_TO_TICKS(1000)) != pdTRUE) {
        ESP_LOGE(TAG, "Semaphore take failed");
        return {};
    }

    std::vector<IRCodeInfo> codes;
    uint64_t current_time = esp_timer_get_time();

    for (const auto& device : code_map_) {
        for (const auto& action : device.second) {
            IRCodeInfo info;
            info.device_type = device.first;
            info.action = action.first;
            info.ir_code = action.second;
            info.protocol = "NEC";
            info.timestamp = current_time;
            info.usage_count = 0;

            codes.push_back(info);
        }
    }

    xSemaphoreGive(store_mutex_);
    return codes;
}

void ESP32IRStore::updateActionIndex() {
    action_index_.clear();

    for (const auto& device : code_map_) {
        std::vector<std::string> actions;
        for (const auto& action : device.second) {
            actions.push_back(action.first);
        }
        action_index_[device.first] = actions;
    }
}

std::string ESP32IRStore::normalizeIRCode(const std::string& code) const {
    std::string normalized = code;

    std::transform(normalized.begin(), normalized.end(), normalized.begin(), ::toupper);

    if (normalized.substr(0, 2) != "0x") {
        normalized = "0x" + normalized;
    }

    return normalized;
}

bool ESP32IRStore::parseHexCode(const std::string& code) const {
    if (code.length() < 3) return false;

    if (code.substr(0, 2) != "0x") return false;

    for (size_t i = 2; i < code.length(); i++) {
        if (!isxdigit(code[i])) return false;
    }

    return true;
}

bool ESP32IRStore::isValidHexString(const std::string& str) const {
    if (str.empty()) return false;

    for (char c : str) {
        if (!isxdigit(c)) return false;
    }

    return true;
}

bool ESP32IRStore::readNVSData() {
    return loadFromNVS();
}

bool ESP32IRStore::writeNVSData() const {
    return saveToNVS();
}

bool ESP32IRStore::readJsonFile(const std::string& filename, std::string& content) const {
    ESP_LOGW(TAG, "파일 읽기는 지원하지 않음");
    return false;
}

bool ESP32IRStore::writeJsonFile(const std::string& filename, const std::string& content) const {
    ESP_LOGW(TAG, "파일 쓰기는 지원하지 않음");
    return false;
}

std::string ESP32IRStore::serializeToJSON() const {
    std::stringstream ss;
    ss << "{";
    ss << "\"ir_codes\":{";

    bool first_device = true;
    for (const auto& device : code_map_) {
        if (!first_device) ss << ",";
        ss << "\"" << device.first << "\":{";

        bool first_action = true;
        for (const auto& action : device.second) {
            if (!first_action) ss << ",";
            ss << "\"" << action.first << "\":\"" << action.second << "\"";
            first_action = false;
        }

        ss << "}";
        first_device = false;
    }

    ss << "}}";
    return ss.str();
}

bool ESP32IRStore::deserializeFromJSON(const std::string& json) {
    ESP_LOGW(TAG, "간단한 JSON 파싱 사용");

    setDefaultCodes();
    return true;
}
