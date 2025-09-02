#pragma once

#include <string>
#include <map>
#include <vector>
#include <memory>
#include <nlohmann/json.hpp>

namespace irremote {

/**
 * @brief 제어신호-IR신호 매핑 정보
 */
struct ControlMapping {
    std::string control_signal;  // 제어신호 (예: "에어컨_전원", "온도_26도")
    std::string ir_signal;       // IR 신호 (예: "0xE0E040BF")
    std::string description;     // 설명
    std::string device_type;     // 기기 타입 (예: "aircon", "tv")
    
    ControlMapping() = default;
    ControlMapping(const std::string& control, const std::string& ir, 
                   const std::string& desc = "", const std::string& type = "")
        : control_signal(control), ir_signal(ir), description(desc), device_type(type) {}
};

/**
 * @brief 제어신호-IR신호 저장소 클래스
 * 
 * 허브에서 받은 제어신호와 IR 신호를 key-value 형태로 저장하고 관리합니다.
 * 제어 요청 시 key로 IR 신호를 가져와서 즉시 전송할 수 있습니다.
 */
class IRCodeStore {
public:
    IRCodeStore() = default;
    ~IRCodeStore() = default;

    // 복사 생성자 및 할당 연산자
    IRCodeStore(const IRCodeStore&) = default;
    IRCodeStore& operator=(const IRCodeStore&) = default;

    // 이동 생성자 및 할당 연산자
    IRCodeStore(IRCodeStore&&) = default;
    IRCodeStore& operator=(IRCodeStore&&) = default;

    /**
     * @brief 제어신호-IR신호 매핑 추가
     * @param control_signal 제어신호 (예: "에어컨_전원")
     * @param ir_signal IR 신호 (예: "0xE0E040BF")
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
     * @return 매칭되는 제어신호들의 벡터
     */
    std::vector<std::string> searchControlSignals(const std::string& pattern) const;

    /**
     * @brief JSON 파일에서 로드
     * @param filename 파일 경로
     * @return 로드 성공 여부
     */
    bool loadFromFile(const std::string& filename);

    /**
     * @brief JSON 파일로 저장
     * @param filename 파일 경로
     * @return 저장 성공 여부
     */
    bool saveToFile(const std::string& filename) const;

    /**
     * @brief JSON 문자열에서 로드
     * @param json_str JSON 문자열
     * @return 로드 성공 여부
     */
    bool loadFromString(const std::string& json_str);

    /**
     * @brief JSON 문자열로 변환
     * @return JSON 문자열
     */
    std::string toJsonString() const;

    /**
     * @brief 저장된 매핑 개수 가져오기
     * @return 총 매핑 개수
     */
    size_t getTotalMappingCount() const;

    /**
     * @brief 기기 타입별 매핑 개수 가져오기
     * @param device_type 기기 타입
     * @return 매핑 개수
     */
    size_t getMappingCountByDevice(const std::string& device_type) const;

    /**
     * @brief 저장소 초기화
     */
    void clear();

    /**
     * @brief 특정 매핑 제거
     * @param control_signal 제어신호
     * @return 제거 성공 여부
     */
    bool removeControlMapping(const std::string& control_signal);

    /**
     * @brief 기기 타입별 매핑 제거
     * @param device_type 기기 타입
     * @return 제거된 매핑 개수
     */
    size_t removeDeviceMappings(const std::string& device_type);

    /**
     * @brief 허브에서 받은 IR 신호 처리
     * @param control_signal 제어신호
     * @param ir_signal IR 신호
     * @param device_type 기기 타입
     */
    void processHubIRSignal(const std::string& control_signal, 
                           const std::string& ir_signal,
                           const std::string& device_type = "");

private:
    // 저장소 구조: control_signal -> ControlMapping
    std::map<std::string, ControlMapping> control_mappings_;
    
    // 역방향 검색을 위한 IR 신호 -> 제어신호 맵
    std::map<std::string, std::string> ir_to_control_map_;
    
    // JSON 파싱 헬퍼 메서드들
    bool parseJsonObject(const nlohmann::json& json_obj);
    void addJsonMapping(const std::string& control_signal, const nlohmann::json& mapping);
};

} // namespace irremote
