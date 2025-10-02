#pragma once

#include <string>
#include <vector>
#include <memory>
#include <mutex>
#include "hardware/irsend.h"

class IRSend;

class Remote {
public:
    Remote(const std::string& name);
    ~Remote() = default;

    Remote(const Remote&) = default;
    Remote& operator=(const Remote&) = default;

    Remote(Remote&&) = default;
    Remote& operator=(Remote&&) = default;

    std::string getName() const;

    IRSendStatus sendControlSignal(const std::string& control_signal);

    std::vector<IRSendStatus> sendControlSignals(const std::vector<std::string>& control_signals,
                                                int delay_ms = 100);
    void setIRSend(std::shared_ptr<IRSend> ir_send);

    std::shared_ptr<IRSend> getIRSend() const;

private:
    std::string name_;
    std::shared_ptr<IRSend> ir_send_;
};

class RemoteManager {
public:
    RemoteManager() = default;
    ~RemoteManager() = default;

    RemoteManager(const RemoteManager&) = delete;
    RemoteManager& operator=(const RemoteManager&) = delete;

    RemoteManager(RemoteManager&&) = default;
    RemoteManager& operator=(RemoteManager&&) = default;

    void addRemote(std::shared_ptr<Remote> remote);

    std::shared_ptr<Remote> getRemote(const std::string& name) const;

    std::vector<std::shared_ptr<Remote>> getAllRemotes() const;

    bool hasRemote(const std::string& name) const;

    bool removeRemote(const std::string& name);

    std::vector<std::string> getAvailableRemoteNames() const;

    size_t getRemoteCount() const;

    void clear();

    IRSendStatus sendControlSignalToRemote(const std::string& remote_name, const std::string& control_signal);

    std::vector<IRSendStatus> sendControlSignalsToRemote(const std::string& remote_name,
                                                        const std::vector<std::string>& control_signals,
                                                        int delay_ms = 100);

private:
    std::vector<std::shared_ptr<Remote>> remotes_;
    mutable std::mutex mutex_;
};
