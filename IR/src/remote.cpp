#include "core/remote.h"
#include <sstream>
#include <iostream>
#include <algorithm>

Remote::Remote(const std::string& name) : name_(name) {}

std::string Remote::getName() const {
    return name_;
}

IRSendStatus Remote::sendControlSignal(const std::string& control_signal) {
    if (!ir_send_) {
        return IRSendStatus::FAILED;
    }
    
    // 여기에 실제 IR 신호 전송 로직 구현
    // 현재는 기본 구현
    return IRSendStatus::SUCCESS;
}

std::vector<IRSendStatus> Remote::sendControlSignals(const std::vector<std::string>& control_signals, int delay_ms) {
    std::vector<IRSendStatus> results;
    
    for (const auto& signal : control_signals) {
        results.push_back(sendControlSignal(signal));
        
        if (delay_ms > 0 && &signal != &control_signals.back()) {
            // 지연 시간 구현 (실제로는 std::this_thread::sleep_for 사용)
        }
    }
    
    return results;
}

void Remote::setIRSend(std::shared_ptr<IRSend> ir_send) {
    ir_send_ = ir_send;
}

std::shared_ptr<IRSend> Remote::getIRSend() const {
    return ir_send_;
}

// RemoteManager 구현
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
