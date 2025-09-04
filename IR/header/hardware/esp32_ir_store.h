#pragma once

#include <string>
#include <map>
#include <vector>
#include <memory>
#include <mutex>
#include <nlohmann/json.hpp>

/**
 * @brief ESP32 IR 신호 저장소 클래스
 * 
 * ESP32에서 IR 신호를 저장하고 관리하는 클래스입니다.
 * 제어신호와 IR 신호 간의 매핑을 제공합니다.
 */
class ESP32IRStore {
public:
    ESP32IRStore() = default;
    ~ESP32IRStore() = default;

    // 복사 생성자 및 할당 연산자
    ESP32IRStore(const ESP32IRStore&) = default;
    ESP32IRStore& operator=(const ESP32IRStore&) = default;

    // 이동 생성자 및 할당 연산자
    ESP32IRStore(ESP32IRStore&&) = default;
    ESP32IRStore& operator=(ESP32IRStore&&) = default;

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
    
    mutable std::mutex mutex_;
};
