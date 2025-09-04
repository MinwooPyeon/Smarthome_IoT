#include <iostream>
#include <signal.h>
#include <chrono>
#include <thread>
#include "hardware/ir_receiver.h"
#include "hardware/appliance_controller.h"
#include "network/mqtt_client.h"
#include "core/config.h"

// 전역 변수
std::atomic<bool> running(true);
IRReceiver* ir_receiver = nullptr;
ApplianceController* appliance_controller = nullptr;
MqttClient* mqtt_client = nullptr;

// 시그널 핸들러
void signalHandler(int signum) {
    std::cout << "\n시그널 수신 (" << signum << "). 프로그램을 종료합니다..." << std::endl;
    running = false;
}

// IR 코드 수신 콜백
void onIRCodeReceived(const std::string& ir_code) {
    std::cout << "IR 코드 수신됨: " << ir_code << std::endl;
    
    if (appliance_controller) {
        // IR 코드로 가전기기 제어
        ControlResult result = appliance_controller->controlAppliance(ir_code);
        
        if (result.success) {
            std::cout << "가전기기 제어 성공: " << result.appliance_id << std::endl;
            
            // MQTT로 상태 전송
            if (mqtt_client && mqtt_client->isConnected()) {
                std::string status_msg = "{\"appliance_id\":\"" + result.appliance_id + 
                                       "\",\"command\":" + std::to_string(static_cast<int>(result.command)) +
                                       ",\"success\":" + (result.success ? "true" : "false") + 
                                       ",\"timestamp\":" + std::to_string(std::chrono::duration_cast<std::chrono::milliseconds>(
                    std::chrono::system_clock::now().time_since_epoch()).count()) + "}";
                
                mqtt_client->publish("irremote/status", status_msg);
            }
        } else {
            std::cerr << "가전기기 제어 실패: " << result.message << std::endl;
        }
    }
}

// 가전기기 제어 콜백
void onControlResult(const ControlResult& result) {
    std::cout << "제어 결과: " << result.appliance_id 
              << " - " << (result.success ? "성공" : "실패") << std::endl;
}

// MQTT 메시지 콜백
void onMQTTMessage(const std::string& topic, const std::string& message) {
    std::cout << "MQTT 메시지 수신: " << topic << " - " << message << std::endl;
    
    try {
        // 간단한 JSON 파싱 (nlohmann/json 없이)
        if (topic == "irremote/control") {
            // 원격 제어 명령 처리
            if (message.find("\"appliance_id\"") != std::string::npos && 
                message.find("\"command\"") != std::string::npos) {
                
                // 간단한 파싱 로직
                size_t id_start = message.find("\"appliance_id\":\"") + 16;
                size_t id_end = message.find("\"", id_start);
                std::string appliance_id = message.substr(id_start, id_end - id_start);
                
                size_t cmd_start = message.find("\"command\":") + 10;
                size_t cmd_end = message.find_first_of(",}", cmd_start);
                int command_value = std::stoi(message.substr(cmd_start, cmd_end - cmd_start));
                ControlCommand command = static_cast<ControlCommand>(command_value);
                
                if (appliance_controller) {
                    ControlResult result = appliance_controller->controlAppliance(appliance_id, command);
                    std::cout << "원격 제어 결과: " << (result.success ? "성공" : "실패") << std::endl;
                }
            }
        }
    } catch (const std::exception& e) {
        std::cerr << "MQTT 메시지 파싱 오류: " << e.what() << std::endl;
    }
}

int main() {
    std::cout << "=== IR 수신기 기반 가전기기 제어 시스템 ===" << std::endl;
    
    // 시그널 핸들러 설정
    signal(SIGINT, signalHandler);
    signal(SIGTERM, signalHandler);
    
    try {
        // 설정 로드
        auto config = Config::loadFromFile("config/app_config.json");
        if (!config) {
            std::cout << "기본 설정으로 시작합니다." << std::endl;
            config = std::make_shared<Config>();
        }
        
        // 가전기기 제어기 초기화
        appliance_controller = new ApplianceController();
        appliance_controller->setControlCallback(onControlResult);
        
        // 설정 파일에서 가전기기 정보 로드
        appliance_controller->loadConfiguration("config/appliances.json");
        
        // IR 수신기 초기화
        int ir_gpio_pin = config->getInt("ir_receiver.gpio_pin", 23);
        ir_receiver = new IRReceiver(ir_gpio_pin);
        ir_receiver->setIRCodeCallback(onIRCodeReceived);
        
        // MQTT 클라이언트 초기화
        mqtt_client = new MqttClient();
        mqtt_client->setMessageCallback(onMQTTMessage);
        
        std::string mqtt_broker = config->getString("mqtt.broker", "localhost");
        int mqtt_port = config->getInt("mqtt.port", 1883);
        
        if (mqtt_client->connect(mqtt_broker, mqtt_port)) {
            std::cout << "MQTT 브로커 연결 성공: " << mqtt_broker << ":" << mqtt_port << std::endl;
            
            // 상태 토픽 구독
            mqtt_client->subscribe("irremote/control");
            mqtt_client->subscribe("irremote/status");
        } else {
            std::cerr << "MQTT 브로커 연결 실패" << std::endl;

        }
        
        // IR 수신 시작
        if (ir_receiver->startReceiving()) {
            std::cout << "IR 수신 시작됨 - GPIO " << ir_gpio_pin << std::endl;
        } else {
            std::cerr << "IR 수신 시작 실패" << std::endl;
            return 1;
        }
        
        std::cout << "시스템이 정상적으로 시작되었습니다." << std::endl;
        std::cout << "IR 센서에서 리모컨 신호를 기다리는 중..." << std::endl;
        std::cout << "종료하려면 Ctrl+C를 누르세요." << std::endl;
        
        // 메인 루프
        while (running) {
            // MQTT 메시지 처리
            if (mqtt_client && mqtt_client->isConnected()) {
                mqtt_client->loop();
            }
            
            // 상태 출력 (10초마다)
            static auto last_status_time = std::chrono::steady_clock::now();
            auto now = std::chrono::steady_clock::now();
            if (std::chrono::duration_cast<std::chrono::seconds>(now - last_status_time).count() >= 10) {
                std::cout << "시스템 상태: IR 수신 중..." << std::endl;
                last_status_time = now;
            }
            

            std::this_thread::sleep_for(std::chrono::milliseconds(100));
        }
        
    } catch (const std::exception& e) {
        std::cerr << "오류 발생: " << e.what() << std::endl;
        return 1;
    }
    
    // 정리 작업
    std::cout << "시스템을 종료합니다..." << std::endl;
    
    if (ir_receiver) {
        ir_receiver->stopReceiving();
        delete ir_receiver;
    }
    
    if (mqtt_client) {
        mqtt_client->disconnect();
        delete mqtt_client;
    }
    
    if (appliance_controller) {
        delete appliance_controller;
    }
    
    std::cout << "시스템이 정상적으로 종료되었습니다." << std::endl;
    return 0;
}
