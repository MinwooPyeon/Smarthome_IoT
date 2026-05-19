#include "app/HubApp.hpp"
#include <csignal>
#include <cstdlib>
#include <string>

int main() {
    const char* host_env = std::getenv("MQTT_HOST");
    const char* port_env = std::getenv("MQTT_PORT");
    const char* id_env   = std::getenv("DEVICE_ID");

    const std::string host = host_env ? host_env : "127.0.0.1";
    const int         port = port_env ? std::stoi(port_env) : 1883;
    const std::string id   = id_env   ? id_env   : "hub-rpi-01";

    HubApp app(id, host, port);
    if (!app.init()) return 1;
    app.run();
    return 0;
}
