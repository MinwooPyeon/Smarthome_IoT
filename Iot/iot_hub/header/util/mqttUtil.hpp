#pragma once
#include <vector>
#include <cstdint>
#include <string>
#include <optional>
#include <chrono>

namespace mqtt{
    enum class connState {DISCONNECTED, CONNECTING, CONNECTED};

    struct pubMsg{
        std::string topic;
        std::vector<uint8_t> payload;
        int qos = 1;
        bool retain = false;
    };

    struct pubResult{
        int mid = 0;
        bool ok = false;
        std::string error;
    };

    struct subSpec {
        std::string topic;
        int qos = 1;
    };

    struct inMsg {
        std::string topic;
        std::vector<uint8_t> payload; // raw bytes
        int  qos    = 0;
        bool retain = false;
    };

    struct mqttConfig{
        std::string brokerAddr = "127.0.0.1";
        int port = 1883;
        std::string clientId = "rpi-pub-001";

        std::optional<std::string> username;
        std::optional<std::string> password;

        int keepaliveSec = 30;
        int cleanSession = true;

        std::optional<std::string> lwtTopic;
        std::optional<std::string> lwtPayload;
        int lwtQos = 1;
        int lwtRetain = true;

        std::chrono::milliseconds backoffInit{1000};
        int maxRetries{3};
        std::chrono::milliseconds backoffMax{32000};

        double pingFactor = 0.5;
    };
}