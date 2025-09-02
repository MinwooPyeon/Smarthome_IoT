#pragma once

#include <string>
#include <vector>
#include <map>
#include <memory>
#include <functional>
#include <nlohmann/json.hpp>

namespace irremote {

/**
 * @brief IR 코드 정보를 담는 구조체
 */
struct Code {
    std::string name;    ///< 코드 이름 (예: KEY_POWER)
    std::string code;    ///< 실제 IR 코드 값
    std::string description; ///< 코드 설명 (선택사항)
    
    Code() = default;
    Code(const std::string& n, const std::string& c, const std::string& desc = "")
        : name(n), code(c), description(desc) {}
};

/**
 * @brief IR 리모컨 클래스
 * 
 * 하나의 리모컨에 대한 모든 IR 코드를 관리합니다.
 */
class Remote {
public:
    explicit Remote(const std::string& name);
    ~Remote() = default;

    // 복사 생성자 및 할당 연산자
    Remote(const Remote&) = default;
    Remote& operator=(const Remote&) = default;

    // 이동 생성자 및 할당 연산자
    Remote(Remote&&) = default;
    Remote& operator=(Remote&&) = default;

    /**
     * @brief IR 코드 추가
     * @param name 코드 이름
     * @param code IR 코드 값
     * @param description 코드 설명 (선택사항)
     */
    void addCode(const std::string& name, const std::string& code, const std::string& description = "");

    /**
     * @brief IR 명령 전송
     * @param command 전송할 명령
     * @return 전송 성공 여부
     */
    bool sendCommand(const std::string& command) const;

    /**
     * @brief 코드 존재 여부 확인
     * @param name 확인할 코드 이름
     * @return 존재 여부
     */
    bool hasCode(const std::string& name) const;

    /**
     * @brief 코드 가져오기
     * @param name 코드 이름
     * @return 코드 객체 (없으면 nullptr)
     */
    const Code* getCode(const std::string& name) const;

    /**
     * @brief 문자열 표현 반환
     * @return 리모컨 정보 문자열
     */
    std::string toString() const;
    
    // Getter 메서드들
    const std::string& getName() const { return name_; }
    const std::vector<Code>& getCodes() const { return codes_; }
    size_t getCodeCount() const { return codes_.size(); }

    /**
     * @brief 코드 검색 (부분 일치)
     * @param pattern 검색 패턴
     * @return 매칭되는 코드들의 벡터
     */
    std::vector<Code> searchCodes(const std::string& pattern) const;

private:
    std::string name_;
    std::vector<Code> codes_;
    std::map<std::string, size_t> code_index_; ///< 코드 이름으로 빠른 검색을 위한 인덱스
};

/**
 * @brief 리모컨 관리자 클래스
 * 
 * 여러 리모컨을 관리하고 명령을 라우팅합니다.
 */
class RemoteManager {
public:
    RemoteManager() = default;
    ~RemoteManager() = default;

    // 복사 생성자 및 할당 연산자
    RemoteManager(const RemoteManager&) = default;
    RemoteManager& operator=(const RemoteManager&) = default;

    // 이동 생성자 및 할당 연산자
    RemoteManager(RemoteManager&&) = default;
    RemoteManager& operator=(RemoteManager&&) = default;

    /**
     * @brief 리모컨 추가
     * @param remote 추가할 리모컨
     */
    void addRemote(const std::shared_ptr<Remote>& remote);

    /**
     * @brief 리모컨 제거
     * @param name 제거할 리모컨 이름
     * @return 제거 성공 여부
     */
    bool removeRemote(const std::string& name);

    /**
     * @brief 리모컨 가져오기
     * @param name 리모컨 이름
     * @return 리모컨 객체 (없으면 nullptr)
     */
    std::shared_ptr<Remote> getRemote(const std::string& name) const;

    /**
     * @brief 명령 전송
     * @param remoteName 리모컨 이름
     * @param command 전송할 명령
     * @return 전송 성공 여부
     */
    bool sendCommand(const std::string& remoteName, const std::string& command) const;

    /**
     * @brief 모든 리모컨 가져오기
     * @return 리모컨 맵
     */
    const std::map<std::string, std::shared_ptr<Remote>>& getAllRemotes() const { return remotes_; }

    /**
     * @brief 리모컨 개수 반환
     * @return 리모컨 개수
     */
    size_t getRemoteCount() const { return remotes_.size(); }

    /**
     * @brief 리모컨 존재 여부 확인
     * @param name 확인할 리모컨 이름
     * @return 존재 여부
     */
    bool hasRemote(const std::string& name) const;

    /**
     * @brief 리모컨 검색 (부분 일치)
     * @param pattern 검색 패턴
     * @return 매칭되는 리모컨들의 맵
     */
    std::map<std::string, std::shared_ptr<Remote>> searchRemotes(const std::string& pattern) const;

    /**
     * @brief 명령 전송 콜백 설정
     * @param callback 콜백 함수
     */
    void setCommandCallback(std::function<void(const std::string&, const std::string&)> callback) {
        command_callback_ = callback;
    }

private:
    std::map<std::string, std::shared_ptr<Remote>> remotes_;
    std::function<void(const std::string&, const std::string&)> command_callback_;
};

} // namespace irremote
