// main.cpp
#include "config.hpp"
#include "mqtt_manager.hpp"
#include <pigpio.h>
#include <thread>
#include <iostream>

int main(){
    if(gpioInitialise() < 0){ std::cerr << "pigpio init failed\n"; return 1; }
    AppConfig cfg;

    MqttManager mgr(cfg);
    if(!mgr.start()){ gpioTerminate(); return 2; }

    while(true) std::this_thread::sleep_for(std::chrono::seconds(1));
    // 도달 X; 필요 시 신호 처리 추가
    mgr.stop();
    gpioTerminate();
    return 0;
}
