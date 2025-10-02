#include "app/HubApp.hpp"
#include <csignal>

int main() {
    HubApp app("hub-rpi-01", "127.0.0.1", 1883);
    if (!app.init()) return 1;
    app.run();
    return 0;
}
