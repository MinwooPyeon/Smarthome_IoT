#include "config.hpp"
#include "mqtt_client.hpp"
#include <iostream>
#include <algorithm>
#include <csignal>
#include <atomic>

static std::atomic<bool> running(true);
static void on_sigint(int){ running=false; }

static std::string arg_or(char** b, char** e, const std::string& k, const std::string& d){
    auto it = std::find(b, e, k);
    if(it!=e && ++it!=e) return std::string(*it);
    return d;
}

int main(int argc, char** argv){
    AppConfig cfg;
    std::string topic = arg_or(argv, argv+argc, "-t", "hub/" + cfg.deviceId + "/#");
    std::cout << topic << "\n"; 
    MqttClient cli;
    if(!cli.init(cfg, "sub_test_" + cfg.deviceId)){
        std::cerr << "init failed\n"; return 1;
    }
    cli.set_message_handler([](const std::string& t, const std::string& p){
        std::cout << "\n[sub] topic: " << t << "\n" << p << "\n";
    });
    if(!cli.subscribe(topic, /*qos*/1)){
        std::cerr << "subscribe failed\n"; return 2;
    }
    std::cout << "MQTT SUBSCIRIBE READY" << std::endl;
    std::signal(SIGINT, on_sigint);
    while(running) cli.loop_for_ms(200);
    cli.cleanup();
    return 0;
}
