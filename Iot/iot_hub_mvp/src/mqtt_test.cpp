// src/mqtt_echo.cpp  (TLS Enabled)
#include "MqttClient.hpp"
#include <iostream>
#include <thread>
#include <atomic>
#include <chrono>
#include <csignal>
#include <sstream>
#include <cstdlib>

static std::atomic<bool> g_running{true};
static void on_sigint(int){ g_running = false; }

static std::string expand_user(const std::string& p){
    if (p.size() >= 2 && p[0]=='~' && (p[1]=='/' || p[1]=='\\')) {
        const char* home = std::getenv("HOME");
        if (home) return std::string(home) + p.substr(1);
    }
    return p;
}

// ---- 내부 기본값 (원하면 여기만 수정) ----
struct Config {
    std::string host   = "43.201.62.254";
    int         port   = 8883;                        // TLS 포트
    std::string topic  = "hub/test-device/env";
    int         qos    = 1;
    std::string client = "cpp-echo-1";

    // 계정 (필요 없으면 빈 문자열)
    std::string user   = "eeum";
    std::string pass   = "ssafy2086eeum";

    // TLS
    std::string cafile = "~/fullchain.pem";          // 서버 CA 체인
    std::string capath = "";                         // 디렉토리 사용 시
    std::string cert   = "";                         // (선택) 클라이언트 인증서
    std::string key    = "";                         // (선택) 클라이언트 키
    bool        insecure = true;                     // IP 접속 시 CN 불일치 허용

    // 동작
    bool   rx_only  = true;                          // 수신 전용
    double interval = 2.0;                           // 퍼블리시 주기(sec), rx_only=false일 때 사용
};

int main(int argc, char** argv)
{
    Config cfg;

    // 간단한 인자 파서
    for (int i=1; i<argc; ++i) {
        std::string a = argv[i];
        auto next = [&](int& i)->std::string { return (i+1<argc)? argv[++i] : ""; };

        if (a=="--host")       cfg.host   = next(i);
        else if (a=="--port")  cfg.port   = std::stoi(next(i));
        else if (a=="--topic") cfg.topic  = next(i);
        else if (a=="--qos")   cfg.qos    = std::stoi(next(i));
        else if (a=="--id")    cfg.client = next(i);
        else if (a=="--user")  cfg.user   = next(i);
        else if (a=="--pass")  cfg.pass   = next(i);

        else if (a=="--cafile") cfg.cafile = next(i);
        else if (a=="--capath") cfg.capath = next(i);
        else if (a=="--cert")   cfg.cert   = next(i);
        else if (a=="--key")    cfg.key    = next(i);
        else if (a=="--insecure") cfg.insecure = true;

        else if (a=="--rx-only") cfg.rx_only = true;
        else if (a=="--tx")      cfg.rx_only = false;
        else if (a=="--interval") cfg.interval = std::stod(next(i));
    }

    std::signal(SIGINT, on_sigint);
    std::signal(SIGTERM, on_sigint);

    // ~ 확장
    std::string cafile = expand_user(cfg.cafile);
    std::string cert   = expand_user(cfg.cert);
    std::string key    = expand_user(cfg.key);

    MqttClient cli;
    // ✅ TLS 초기화
    if (!cli.init_with_tls(cfg.client, cfg.host, cfg.port,
                           cafile, cfg.capath, cert, key,
                           cfg.insecure, cfg.user, cfg.pass)) {
        std::cerr << "init_with_tls failed\n";
        return 1;
    }

    cli.set_message_handler([](const std::string& t, const std::string& p){
        std::cout << "[MSG] " << t << " | " << p << "\n";
    });

    if (!cli.subscribe(cfg.topic, cfg.qos)) {
        std::cerr << "subscribe failed\n";
        return 2;
    }

    std::thread pub;
    if (!cfg.rx_only) {
        pub = std::thread([&](){
            int seq = 0;
            while (g_running) {
                std::ostringstream oss;
                oss << R"({"seq":)" << seq++
                    << R"(,"ts":)" << std::chrono::duration_cast<std::chrono::milliseconds>(
                        std::chrono::system_clock::now().time_since_epoch()).count()
                    << R"(,"msg":"hello from C++"})";
                cli.publish(cfg.topic, oss.str(), cfg.qos, false);
                std::this_thread::sleep_for(std::chrono::milliseconds((int)(cfg.interval*1000)));
            }
        });
    }

    cli.loop_forever();

    g_running = false;
    if (pub.joinable()) pub.join();
    cli.cleanup();
    return 0;
}
