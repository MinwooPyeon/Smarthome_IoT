#include "core/remote.h"
#include "hardware/irsend.h"
#include <sstream>
#include <iostream>
#include <algorithm>
#include <thread>
#include <chrono>

Remote::Remote(const std::string& name) : name_(name) {}

std::string Remote::getName() const {
    return name_;
}

IRSendStatus Remote::sendControlSignal(const std::string& control_signal) {
    if (!ir_send_) {
        std::cerr << "Remote " << name_ << ": IR 송신기가 설정되지 않음" << std::endl;
        return IRSendStatus(IRSendResult::DEVICE_NOT_FOUND, "IR 송신기가 설정되지 않음");
    }

    if (control_signal.empty()) {
        std::cerr << "Remote " << name_ << ": 빈 제어 신호" << std::endl;
        return IRSendStatus(IRSendResult::INVALID_CODE, "빈 제어 신호");
    }

    std::cout << "Remote " << name_ << ": IR 신호 전송 시작 - " << control_signal << std::endl;

    auto result = ir_send_->sendControlSignal(control_signal);

    if (result.result == IRSendResult::SUCCESS) {
        std::cout << "Remote " << name_ << ": IR 신호 전송 성공" << std::endl;
        return result;
    } else {
        std::cerr << "Remote " << name_ << ": IR 신호 전송 실패 - " << result.message << std::endl;
        return result;
    }
}

std::vector<IRSendStatus> Remote::sendControlSignals(const std::vector<std::string>& control_signals, int delay_ms) {
    std::vector<IRSendStatus> results;

    if (control_signals.empty()) {
        std::cerr << "Remote " << name_ << ": 빈 제어 신호 목록" << std::endl;
        return results;
    }

    std::cout << "Remote " << name_ << ": " << control_signals.size() << "개 IR 신호 전송 시작" << std::endl;

    for (size_t i = 0; i < control_signals.size(); ++i) {
        const auto& signal = control_signals[i];

        std::cout << "Remote " << name_ << ": 신호 " << (i + 1) << "/" << control_signals.size() << " 전송 중..." << std::endl;

        IRSendStatus result = sendControlSignal(signal);
        results.push_back(result);


        if (i < control_signals.size() - 1 && delay_ms > 0) {
            std::cout << "Remote " << name_ << ": " << delay_ms << "ms 대기 중..." << std::endl;
            std::this_thread::sleep_for(std::chrono::milliseconds(delay_ms));
        }
    }

    int success_count = 0;
    for (const auto& result : results) {
        if (result.result == IRSendResult::SUCCESS) {
            success_count++;
        }
    }

    std::cout << "Remote " << name_ << ": 전송 완료 - " << success_count << "/" << control_signals.size() << " 성공" << std::endl;

    return results;
}

void Remote::setIRSend(std::shared_ptr<IRSend> ir_send) {
    ir_send_ = ir_send;
}

std::shared_ptr<IRSend> Remote::getIRSend() const {
    return ir_send_;
}

void RemoteManager::addRemote(std::shared_ptr<Remote> remote) {
    std::lock_guard<std::mutex> lock(mutex_);
    remotes_.push_back(remote);
}

std::shared_ptr<Remote> RemoteManager::getRemote(const std::string& name) const {
    std::lock_guard<std::mutex> lock(mutex_);

    for (const auto& remote : remotes_) {
        if (remote->getName() == name) {
            return remote;
        }
    }
    return nullptr;
}

std::vector<std::shared_ptr<Remote>> RemoteManager::getAllRemotes() const {
    std::lock_guard<std::mutex> lock(mutex_);
    return remotes_;
}

bool RemoteManager::hasRemote(const std::string& name) const {
    return getRemote(name) != nullptr;
}

bool RemoteManager::removeRemote(const std::string& name) {
    std::lock_guard<std::mutex> lock(mutex_);

    auto it = std::remove_if(remotes_.begin(), remotes_.end(),
        [&name](const std::shared_ptr<Remote>& remote) {
            return remote->getName() == name;
        });

    if (it != remotes_.end()) {
        remotes_.erase(it, remotes_.end());
        return true;
    }
    return false;
}

std::vector<std::string> RemoteManager::getAvailableRemoteNames() const {
    std::lock_guard<std::mutex> lock(mutex_);

    std::vector<std::string> names;
    for (const auto& remote : remotes_) {
        names.push_back(remote->getName());
    }
    return names;
}

size_t RemoteManager::getRemoteCount() const {
    std::lock_guard<std::mutex> lock(mutex_);
    return remotes_.size();
}

void RemoteManager::clear() {
    std::lock_guard<std::mutex> lock(mutex_);
    remotes_.clear();
}

IRSendStatus RemoteManager::sendControlSignalToRemote(const std::string& remote_name, const std::string& control_signal) {
    auto remote = getRemote(remote_name);
    if (!remote) {
        std::cerr << "RemoteManager: 리모컨을 찾을 수 없음 - " << remote_name << std::endl;
        return IRSendStatus(IRSendResult::DEVICE_NOT_FOUND, "리모컨을 찾을 수 없음");
    }

    return remote->sendControlSignal(control_signal);
}

std::vector<IRSendStatus> RemoteManager::sendControlSignalsToRemote(const std::string& remote_name,
                                                                   const std::vector<std::string>& control_signals,
                                                                   int delay_ms) {
    auto remote = getRemote(remote_name);
    if (!remote) {
        std::cerr << "RemoteManager: 리모컨을 찾을 수 없음 - " << remote_name << std::endl;
        return std::vector<IRSendStatus>();
    }

    return remote->sendControlSignals(control_signals, delay_ms);
}
