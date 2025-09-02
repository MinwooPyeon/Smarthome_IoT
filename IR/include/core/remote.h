#pragma once

#include <string>
#include <vector>
#include <memory>
#include "hardware/irsend.h"

namespace irremote {

/**
 * @brief 리모컨 클래스
 * 
 * 제어신호-IR신호 매핑을 통해 기기를 제어합니다.
 * 허브에서 받은 제어신호를 IR 신호로 변환하여 전송할 수 있습니다.
 */
class Remote {
public:
    Remote(const std::string& name);
    ~Remote() = default;
    
    // 복사 생성자 및 할당 연산자
    Remote(const Remote&) = default;
    Remote& operator=(const Remote&) = default;
    
    // 이동 생성자 및 할당 연산자
    Remote(Remote&&) = default;
    Remote& operator=(Remote&&) = default;

    /**
     * @brief 리모컨 이름 가져오기
     * @return 리모컨 이름
     */
    std::string getName() const;

    /**
     * @brief 단일 제어신호 전송
     * @param control_signal 제어신호
     * @return 전송 결과
     */
    IRSendStatus sendControlSignal(const std::string& control_signal);

    /**
     * @brief 여러 제어신호 순차 전송
     * @param control_signals 제어신호 목록
     * @param delay_ms 명령 간 지연 시간 (밀리초)
     * @return 전송 결과들
     */
    std::vector<IRSendStatus> sendControlSignals(const std::vector<std::string>& control_signals,
                                                int delay_ms = 100);

    /**
     * @brief IRSend 객체 설정
     * @param ir_send IRSend 객체 포인터
     */
    void setIRSend(std::shared_ptr<IRSend> ir_send);

    /**
     * @brief IRSend 객체 가져오기
     * @return IRSend 객체 포인터
     */
    std::shared_ptr<IRSend> getIRSend() const;

private:
    std::string name_;
    std::shared_ptr<IRSend> ir_send_;
};

/**
 * @brief 리모컨 관리자 클래스
 * 
 * 여러 리모컨을 관리하고 제어신호를 적절한 리모컨으로 라우팅합니다.
 */
class RemoteManager {
public:
    RemoteManager() = default;
    ~RemoteManager() = default;
    
    // 복사 생성자 및 할당 연산자 (삭제)
    RemoteManager(const RemoteManager&) = delete;
    RemoteManager& operator=(const RemoteManager&) = delete;
    
    // 이동 생성자 및 할당 연산자
    RemoteManager(RemoteManager&&) = default;
    RemoteManager& operator=(RemoteManager&&) = default;

    /**
     * @brief 리모컨 추가
     * @param remote 리모컨 객체
     */
    void addRemote(std::shared_ptr<Remote> remote);

    /**
     * @brief 리모컨 가져오기
     * @param name 리모컨 이름
     * @return 리모컨 객체 포인터 (없으면 nullptr)
     */
    std::shared_ptr<Remote> getRemote(const std::string& name) const;

    /**
     * @brief 모든 리모컨 가져오기
     * @return 리모컨 객체 목록
     */
    std::vector<std::shared_ptr<Remote>> getAllRemotes() const;

    /**
     * @brief 리모컨 존재 여부 확인
     * @param name 리모컨 이름
     * @return 존재 여부
     */
    bool hasRemote(const std::string& name) const;

    /**
     * @brief 리모컨 제거
     * @param name 리모컨 이름
     * @return 제거 성공 여부
     */
    bool removeRemote(const std::string& name);

    /**
     * @brief 사용 가능한 리모컨 이름 목록 가져오기
     * @return 리모컨 이름 목록
     */
    std::vector<std::string> getAvailableRemoteNames() const;

    /**
     * @brief 저장된 리모컨 개수 가져오기
     * @return 리모컨 개수
     */
    size_t getRemoteCount() const;

    /**
     * @brief 모든 리모컨 제거
     */
    void clear();

private:
    std::vector<std::shared_ptr<Remote>> remotes_;
    mutable std::mutex mutex_;
};

} // namespace irremote
