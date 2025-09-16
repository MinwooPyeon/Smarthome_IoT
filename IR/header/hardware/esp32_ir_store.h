#pragma once

#include <string>
#include <map>
#include <vector>
#include <memory>
#include <mutex>
#include "nlohmann/json.hpp"
#include "nvs_flash.h"
#include "nvs.h"
#include "freertos/FreeRTOS.h"
#include "freertos/semphr.h"

/**
 * @brief ESP32 IR 신호 저장소 클래스
 *
 * ESP32에서 IR 신호를 저장하고 관리하는 클래스입니다.
 * 제어신호와 IR 신호 간의 매핑을 제공합니다.
 */
class ESP32IRStore {
public:
    ESP32IRStore();
    ~ESP32IRStore();

    // 복사 생성자 및 할당 연산자
    ESP32IRStore(const ESP32IRStore&) = default;
    ESP32IRStore& operator=(const ESP32IRStore&) = default;

    // 이동 생성자 및 할당 연산자
    ESP32IRStore(ESP32IRStore&&) = default;
    ESP32IRStore& operator=(ESP32IRStore&&) = default;

    // 초기화 및 정리
    bool initialize();
    void cleanup();

    // IR 코드 저장 및 조회
    bool storeCode(const std::string& device_type, const std::string& action, const std::string& ir_code);
    std::string getCode(const std::string& device_type, const std::string& action) const;

    // 기기 및 액션 관리
    std::vector<std::string> getDeviceTypes() const;
    std::vector<std::string> getActions(const std::string& device_type) const;

    // NVS 저장/로드
    bool loadFromNVS();
    bool saveToNVS() const;

    // 기본 코드 설정
    void setDefaultCodes();

    // 기기 및 액션 제거
    bool removeDevice(const std::string& device_type);
    bool removeAction(const std::string& device_type, const std::string& action);

    // 코드 개수 조회
    size_t getCodeCount() const;

    // 모든 데이터 지우기
    void clear();

    // 코드 검증
    bool isValidCode(const std::string& ir_code) const;

    // 디버그 정보 출력
    void printDebugInfo() const;

    // 코드 존재 여부 확인
    bool codeExists(const std::string& ir_code) const;

    // 코드로 기기 및 액션 찾기
    std::string findDeviceByCode(const std::string& ir_code) const;
    std::string findActionByCode(const std::string& ir_code) const;

    // 모든 코드 정보 가져오기
    struct IRCodeInfo {
        std::string device_type;
        std::string action;
        std::string ir_code;
        std::string description;
        std::string protocol;
        uint32_t timestamp;
        int usage_count;
    };
    std::vector<IRCodeInfo> getAllCodes() const;

    /**
     * @brief 제어신호-IR신호 매핑 추가
     * @param control_signal 제어신호
     * @param ir_signal IR 신호
     * @param description 설명
     * @param device_type 기기 타입
     */
    void addControlMapping(const std::string& control_signal,
                          const std::string& ir_signal,
                          const std::string& description = "",
                          const std::string& device_type = "");

    /**
     * @brief 제어신호로 IR 신호 가져오기
     * @param control_signal 제어신호
     * @return IR 신호 (없으면 빈 문자열)
     */
    std::string getIRSignal(const std::string& control_signal) const;

    /**
     * @brief IR 신호로 제어신호 가져오기
     * @param ir_signal IR 신호
     * @return 제어신호 (없으면 빈 문자열)
     */
    std::string getControlSignal(const std::string& ir_signal) const;

    /**
     * @brief 제어신호 존재 여부 확인
     * @param control_signal 제어신호
     * @return 존재 여부
     */
    bool hasControlSignal(const std::string& control_signal) const;

    /**
     * @brief IR 신호 존재 여부 확인
     * @param ir_signal IR 신호
     * @return 존재 여부
     */
    bool hasIRSignal(const std::string& ir_signal) const;

    /**
     * @brief 모든 제어신호 목록 가져오기
     * @return 제어신호 목록
     */
    std::vector<std::string> getAllControlSignals() const;

    /**
     * @brief 기기 타입별 제어신호 목록 가져오기
     * @param device_type 기기 타입
     * @return 제어신호 목록
     */
    std::vector<std::string> getControlSignalsByDevice(const std::string& device_type) const;

    /**
     * @brief 제어신호 검색 (부분 일치)
     * @param pattern 검색 패턴
     * @return 매칭되는 제어신호 목록
     */
    std::vector<std::string> searchControlSignals(const std::string& pattern) const;

    /**
     * @brief 매핑 정보 제거
     * @param control_signal 제어신호
     * @return 성공 여부
     */
    bool removeControlMapping(const std::string& control_signal);

    /**
     * @brief 모든 매핑 정보 제거
     */
    void clearAllMappings();

    /**
     * @brief 매핑 정보 개수
     * @return 매핑 정보 개수
     */
    size_t getMappingCount() const;

    /**
     * @brief JSON 파일에서 매핑 정보 로드
     * @param filename JSON 파일 경로
     * @return 성공 여부
     */
    bool loadFromFile(const std::string& filename);

    /**
     * @brief JSON 파일로 매핑 정보 저장
     * @param filename JSON 파일 경로
     * @return 성공 여부
     */
    bool saveToFile(const std::string& filename) const;

    /**
     * @brief JSON 문자열에서 매핑 정보 로드
     * @param json_str JSON 문자열
     * @return 성공 여부
     */
    bool loadFromJSON(const std::string& json_str);

    /**
     * @brief JSON 문자열로 매핑 정보 변환
     * @return JSON 문자열
     */
    std::string toJSON() const;

private:
    std::map<std::string, std::string> control_to_ir_mapping_;
    std::map<std::string, std::string> ir_to_control_mapping_;
    std::map<std::string, std::string> descriptions_;
    std::map<std::string, std::string> device_types_;

    // IR 코드 저장소
    std::map<std::string, std::map<std::string, std::string>> code_map_;
    std::map<std::string, std::vector<std::string>> action_index_;
    std::map<std::string, std::map<std::string, std::string>> device_actions_;
    std::map<std::string, std::string> action_descriptions_;

    SemaphoreHandle_t store_mutex_;
    nvs_handle_t nvs_handle_;

    // NVS 관련 상수
    static const char* NVS_NAMESPACE;
    static const char* NVS_KEY_IR_CODES;

    // 내부 메서드
    void updateActionIndex();
    std::string normalizeIRCode(const std::string& code) const;
    bool parseHexCode(const std::string& code) const;
    bool isValidHexString(const std::string& str) const;

    // NVS 파일 시스템 메서드
    bool readNVSData();
    bool writeNVSData() const;
    bool readJsonFile(const std::string& filename, std::string& content) const;
    bool writeJsonFile(const std::string& filename, const std::string& content) const;
    std::string serializeToJSON() const;
    bool deserializeFromJSON(const std::string& json);
};
