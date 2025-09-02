#pragma once

#include <string>
#include <vector>
#include <map>
#include <memory>
#include "proto/irremote.pb.h"

namespace irremote {

struct Code {
    std::string name;
    std::string code;
};

class Remote {
public:
    Remote(const std::string& name);
    ~Remote() = default;

    void addCode(const std::string& name, const std::string& code);
    bool sendCommand(const std::string& command) const;
    std::string toString() const;
    
    const std::string& getName() const { return name_; }
    const std::vector<Code>& getCodes() const { return codes_; }

private:
    std::string name_;
    std::vector<Code> codes_;
};

class RemoteManager {
public:
    RemoteManager() = default;
    ~RemoteManager() = default;

    void addRemote(const std::shared_ptr<Remote>& remote);
    std::shared_ptr<Remote> getRemote(const std::string& name) const;
    bool sendCommand(const std::string& remoteName, const std::string& command) const;
    std::map<std::string, std::shared_ptr<Remote>> getAllRemotes() const { return remotes_; }

private:
    std::map<std::string, std::shared_ptr<Remote>> remotes_;
};

} // namespace irremote
