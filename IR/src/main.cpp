#include <iostream>
#include <string>
#include <thread>
#include <chrono>
#include <signal.h>
#include "core/config.h"
#include "hardware/ir_receiver.h"
#include "hardware/appliance_controller.h"
#include "network/matter_client.h"
#include "../external/nlohmann/json.hpp"

using json = nlohmann::json;

// 전역 변수
Config* config = nullptr;
IRReceiver* ir_receiver = nullptr;
ApplianceController* appliance_controller = nullptr;
MatterClient* matter_client = nullptr;

// 시그널 핸들러
void signalHandler(int signum) {
    std::cout << "시그널 " << signum << " 수신. 프로그램을 종료합니다." << std::endl;
    exit(signum);
}

// IR 코드 수신 콜백
void onIRCodeReceived(const std::string& ir_code) {
    std::cout << "IR 코드 수신: " << ir_code << std::endl;
    
    if (appliance_controller) {
        // IR 코드를 디바이스 명령으로 변환
        auto result = appliance_controller->controlAppliance(ir_code);
        if (result.success) {
            std::cout << "디바이스 제어 성공: " << result.appliance_id << std::endl;
            
            // Matter 디바이스에 명령 전송
            if (matter_client && matter_client->isConnected()) {
                matter_client->sendCommand(result.appliance_id, static_cast<int>(result.command));
            }
            
            // MQTT로 상태 업데이트 전송은 비활성화됨
        } else {
            std::cout << "디바이스 제어 실패: " << result.message << std::endl;
        }
    }
}

// MQTT 메시지 수신 콜백은 비활성화됨

int main() {
    std::cout << "=== Raspberry Pi IR Remote Control System ===" << std::endl;
    std::cout << "버전: 1.0.0" << std::endl;
    std::cout << "플랫폼: Windows (MinGW)" << std::endl;
    
    // 시그널 핸들러 설정
    signal(SIGINT, signalHandler);
    signal(SIGTERM, signalHandler);
    
    try {
        // 설정 로드
        config = new Config(); // 동적 할당
        std::cout << "기본 설정으로 시작합니다." << std::endl;
        
        // 가전기기 제어기 초기화
        appliance_controller = new ApplianceController(); // 동적 할당
        appliance_controller->setControlCallback([](const ControlResult& result) {
            std::cout << "제어 결과: " << result.appliance_id 
                      << " - " << (result.success ? "성공" : "실패") << std::endl;
        });
        
        // 설정 파일에서 가전기기 정보 로드
        appliance_controller->loadConfiguration("config/appliances.json");
        
        // IR 수신기 초기화
        int ir_gpio_pin = 23; // 기본값
        if (config) {
            // config에서 GPIO 핀 읽기
        }
        ir_receiver = new IRReceiver(ir_gpio_pin); // 동적 할당
        ir_receiver->setIRCodeCallback(onIRCodeReceived);
        
        // MQTT 클라이언트는 비활성화됨
        
        // Matter 클라이언트 초기화
        matter_client = new MatterClient(); // 동적 할당
        matter_client->setDebugMode(true);
        
        if (matter_client->initialize("fabric_001", "node_001")) {
            if (matter_client->connect()) {
                std::cout << "Matter 네트워크 연결 성공" << std::endl;
                
                // 디바이스 상태 구독
                matter_client->subscribeToDeviceStatus("matter_aircon_001", [](const std::string& device_id, const std::map<std::string, std::string>& status) {
                    std::cout << "Matter 디바이스 상태 변경: " << device_id << std::endl;
                    for (const auto& [key, value] : status) {
                        std::cout << "  " << key << ": " << value << std::endl;
                    }
                });
                
                // 발견된 디바이스들을 Matter 클라이언트에 추가
                auto devices = matter_client->discoverDevices(5000);
                for (const auto& device : devices) {
                    matter_client->addDevice(device);
                }
                
                std::cout << "Matter 디바이스 " << devices.size() << "개 등록 완료" << std::endl;
            } else {
                std::cerr << "Matter 네트워크 연결 실패: " << matter_client->getLastError() << std::endl;
            }
        } else {
            std::cerr << "Matter 클라이언트 초기화 실패" << std::endl;
        }
        
        // IR 수신 시작
        if (ir_receiver->startReceiving()) {
            std::cout << "IR 수신 시작됨 - GPIO " << ir_gpio_pin << std::endl;
        } else {
            std::cerr << "IR 수신 시작 실패" << std::endl;
        }
        
        std::cout << "시스템이 정상적으로 시작되었습니다." << std::endl;
        std::cout << "Ctrl+C를 눌러 프로그램을 종료할 수 있습니다." << std::endl;
        
        // Matter 디바이스 상태 확인 주기
        auto last_matter_check = std::chrono::steady_clock::now();
        
        // 메인 루프
        while (true) { // running 변수 제거
            // MQTT 메시지 처리는 비활성화됨
            
            // Matter 디바이스 상태 확인 (30초마다)
            auto now = std::chrono::steady_clock::now();
            if (std::chrono::duration_cast<std::chrono::seconds>(now - last_matter_check).count() >= 30) {
                if (matter_client && matter_client->isConnected()) {
                    std::vector<MatterDevice> devices = matter_client->discoverDevices(5000);
                    std::cout << "Matter 디바이스 검색 완료: " << devices.size() << "개 발견" << std::endl;
                    
                    // 디바이스 상태 업데이트
                    for (const auto& device : devices) {
                        matter_client->addDevice(device);
                    }
                }
                last_matter_check = now;
            }
            
            // CPU 사용량을 줄이기 위한 짧은 대기
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
        }
        
    } catch (const std::exception& e) {
        std::cerr << "치명적 오류 발생: " << e.what() << std::endl;
        return 1;
    }
    
    // 정리 작업
    if (ir_receiver) {
        ir_receiver->stopReceiving();
        delete ir_receiver; // 제거
    }
    
    // MQTT 클라이언트는 비활성화됨
    
    if (matter_client) {
        // delete matter_client; // 제거
    }
    
    if (appliance_controller) {
        // delete appliance_controller; // 제거
    }
    
    if (config) {
        delete config; // 제거
    }
    
    std::cout << "시스템이 정상적으로 종료되었습니다." << std::endl;
    return 0;
}
