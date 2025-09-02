#include "remote.h"
#include "irsend.h"
#include <sstream>
#include <iostream>

namespace irremote {

Remote::Remote(const std::string& name) : name_(name) {}

void Remote::addCode(const std::string& name, const std::string& code) {
    codes_.push_back({name, code});
}

bool Remote::sendCommand(const std::string& command) const {
    // Find the code for the command
    std::string code;
    for (const auto& c : codes_) {
        if (c.name == command) {
            code = c.code;
            break;
        }
    }

    if (code.empty()) {
        std::cerr << "Remote " << name_ << " does not have command " << command << std::endl;
        return false;
    }

    return irsend::Send(name_, code);
}

std::string Remote::toString() const {
    std::ostringstream oss;
    oss << "Name: " << name_ << "\n";
    oss << "Command name\tCode\n";
    
    for (const auto& code : codes_) {
        oss << code.name << "\t" << code.code << "\n";
    }
    
    return oss.str();
}

void RemoteManager::addRemote(const std::shared_ptr<Remote>& remote) {
    remotes_[remote->getName()] = remote;
}

std::shared_ptr<Remote> RemoteManager::getRemote(const std::string& name) const {
    auto it = remotes_.find(name);
    if (it != remotes_.end()) {
        return it->second;
    }
    return nullptr;
}

bool RemoteManager::sendCommand(const std::string& remoteName, const std::string& command) const {
    auto remote = getRemote(remoteName);
    if (!remote) {
        std::cerr << "No remote with name " << remoteName << std::endl;
        return false;
    }
    
    return remote->sendCommand(command);
}

} // namespace irremote
