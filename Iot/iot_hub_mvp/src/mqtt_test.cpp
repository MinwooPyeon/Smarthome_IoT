#include "MqttClient.hpp"
#include <iostream>
#include <thread>
#include <atomic>
#include <chrono>
#include <csignal>
#include <sstream>

static std::atomic<bool> g_running{true};

static void on_sigint(int){ g_running = false; }

int main(int argc, char** argv)
{
    std::string host = "localhost";
    int port = 1883;
    std::string topic = "test/echo";
    int qos = 0;
    std::string clientId = "cpp-echo";
    std::string user, pass;

    // 아주 단순한 인자 파싱
    for (int i=1; i<argc; ++i) {
        std::string a = argv[i];
        auto next = [&](int& i)->std::string { return (i+1<argc)? argv[++i] : ""; };
        if (a=="--host") host = next(i);
        else if (a=="--port") port = std::stoi(next(i));
        else if (a=="--topic") topic = next(i);
        else if (a=="--qos") qos = std::stoi(next(i));
        else if (a=="--id") clientId = next(i);
        else if (a=="--user") user = next(i);
        else if (a=="--pass") pass = next(i);
    }

    std::signal(SIGINT, on_sigint);
    std::signal(SIGTERM, on_sigint);

    MqttClient cli;
    if (!cli.init(clientId, host, port, user, pass)) {
        std::cerr << "init failed\n";
        return 1;
    }

    cli.set_message_handler([](const std::string& t, const std::string& p){
        std::cout << "[MSG] " << t << " | " << p << "\n";
    });

    if (!cli.subscribe(topic, qos)) {
        std::cerr << "subscribe failed\n";
        return 2;
    }

    // 퍼블리셔 스레드(2초마다 전송)
    std::thread pub([&](){
        int seq = 0;
        while (g_running) {
            std::ostringstream oss;
            oss << R"({"seq":)" << seq++
                << R"(,"ts":)" << std::chrono::duration_cast<std::chrono::milliseconds>(
                                   std::chrono::system_clock::now().time_since_epoch()).count()
                << R"(,"msg":"hello from C++"})";
            cli.publish(topic, oss.str(), qos, /*retain*/false);
            std::this_thread::sleep_for(std::chrono::seconds(2));
        }
    });

    // 네트워크 루프(블로킹) → 별도 스레드에서 SIGINT 들어오면 disconnect되어 빠져나옴
    cli.loop_forever();

    g_running = false;
    if (pub.joinable()) pub.join();
    cli.cleanup();
    return 0;
}
