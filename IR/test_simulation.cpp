#include <iostream>
#include <string>
#include <thread>
#include <chrono>
#include <map>

// Windows 시뮬레이션용 MQTT 클라이언트
class SimulatedMqttClient {
private:
    bool connected = false;
    std::string broker;
    int port;
    std::string client_id;

public:
    SimulatedMqttClient() {
        client_id = "irremote_simulator";
        std::cout << "🔧 MQTT 클라이언트 시뮬레이션 초기화" << std::endl;
    }

    bool connect(const std::string& broker, int port) {
        this->broker = broker;
        this->port = port;
        connected = true;
        
        std::cout << "✅ MQTT 연결 시뮬레이션: " << broker << ":" << port << std::endl;
        std::cout << "   클라이언트 ID: " << client_id << std::endl;
        return true;
    }

    bool publish(const std::string& topic, const std::string& message) {
        if (!connected) {
            std::cout << "❌ MQTT 연결되지 않음" << std::endl;
            return false;
        }
        
        std::cout << "📤 MQTT 발행: " << topic << " -> " << message << std::endl;
        return true;
    }

    bool subscribe(const std::string& topic) {
        if (!connected) {
            std::cout << "❌ MQTT 연결되지 않음" << std::endl;
            return false;
        }
        
        std::cout << "📥 MQTT 구독: " << topic << std::endl;
        return true;
    }

    void disconnect() {
        connected = false;
        std::cout << "🔌 MQTT 연결 해제" << std::endl;
    }
};

// IR 코드 시뮬레이션
class SimulatedIRController {
private:
    std::map<std::string, std::string> ir_codes;

public:
    SimulatedIRController() {
        // 샘플 IR 코드들
        ir_codes["samsung_tv_power"] = "0xE0E040BF";
        ir_codes["samsung_tv_volume_up"] = "0xE0E0E01F";
        ir_codes["samsung_tv_volume_down"] = "0xE0E0D02F";
        ir_codes["samsung_ac_power"] = "0xE0E040BF";
        ir_codes["samsung_ac_temp_up"] = "0xE0E01CE3";
        ir_codes["samsung_ac_temp_down"] = "0xE0E05CA3";
        
        std::cout << "🔧 IR 컨트롤러 시뮬레이션 초기화" << std::endl;
        std::cout << "   등록된 IR 코드: " << ir_codes.size() << "개" << std::endl;
    }

    bool sendIRCode(const std::string& device, const std::string& command) {
        std::string key = device + "_" + command;
        
        if (ir_codes.find(key) != ir_codes.end()) {
            std::cout << "📡 IR 신호 전송: " << device << " -> " << command 
                      << " (코드: " << ir_codes[key] << ")" << std::endl;
            return true;
        } else {
            std::cout << "❌ 알 수 없는 IR 코드: " << key << std::endl;
            return false;
        }
    }

    void listAvailableCodes() {
        std::cout << "\n📋 사용 가능한 IR 코드:" << std::endl;
        for (const auto& pair : ir_codes) {
            std::cout << "   " << pair.first << " -> " << pair.second << std::endl;
        }
    }
};

// 메인 시뮬레이션 클래스
class IRRemoteSimulator {
private:
    SimulatedMqttClient mqtt;
    SimulatedIRController ir;

public:
    void initialize() {
        std::cout << "\n🚀 IR Remote 시뮬레이션 시작" << std::endl;
        std::cout << "=================================" << std::endl;
        
        // MQTT 연결
        mqtt.connect("192.168.1.100", 1883);
        mqtt.subscribe("irremote/command");
        mqtt.subscribe("irremote/status");
        
        // 상태 메시지 발행
        mqtt.publish("irremote/status", "simulator_ready");
        
        std::cout << "\n✅ 시뮬레이션 초기화 완료" << std::endl;
    }

    void runSimulation() {
        std::cout << "\n🎮 시뮬레이션 모드 시작" << std::endl;
        std::cout << "명령어를 입력하세요 (help: 도움말, quit: 종료)" << std::endl;
        
        std::string input;
        while (true) {
            std::cout << "\n> ";
            std::getline(std::cin, input);
            
            if (input == "quit" || input == "exit") {
                break;
            } else if (input == "help") {
                showHelp();
            } else if (input == "list") {
                ir.listAvailableCodes();
            } else if (input == "test") {
                runTestSequence();
            } else if (input.find("send ") == 0) {
                // "send device command" 형식
                std::string command = input.substr(5);
                size_t space_pos = command.find(' ');
                if (space_pos != std::string::npos) {
                    std::string device = command.substr(0, space_pos);
                    std::string cmd = command.substr(space_pos + 1);
                    ir.sendIRCode(device, cmd);
                } else {
                    std::cout << "❌ 사용법: send <device> <command>" << std::endl;
                }
            } else {
                std::cout << "❌ 알 수 없는 명령어: " << input << std::endl;
                std::cout << "   'help'를 입력하여 도움말을 확인하세요." << std::endl;
            }
        }
        
        std::cout << "\n👋 시뮬레이션 종료" << std::endl;
        mqtt.disconnect();
    }

private:
    void showHelp() {
        std::cout << "\n📖 도움말:" << std::endl;
        std::cout << "  help          - 이 도움말 표시" << std::endl;
        std::cout << "  list          - 사용 가능한 IR 코드 목록" << std::endl;
        std::cout << "  test          - 테스트 시퀀스 실행" << std::endl;
        std::cout << "  send <device> <command> - IR 신호 전송" << std::endl;
        std::cout << "  quit/exit     - 프로그램 종료" << std::endl;
        std::cout << "\n예시:" << std::endl;
        std::cout << "  send samsung_tv power" << std::endl;
        std::cout << "  send samsung_ac temp_up" << std::endl;
    }

    void runTestSequence() {
        std::cout << "\n🧪 테스트 시퀀스 실행" << std::endl;
        
        // TV 테스트
        std::cout << "\n📺 TV 테스트:" << std::endl;
        ir.sendIRCode("samsung_tv", "power");
        std::this_thread::sleep_for(std::chrono::milliseconds(500));
        ir.sendIRCode("samsung_tv", "volume_up");
        std::this_thread::sleep_for(std::chrono::milliseconds(500));
        ir.sendIRCode("samsung_tv", "volume_down");
        
        // 에어컨 테스트
        std::cout << "\n❄️ 에어컨 테스트:" << std::endl;
        ir.sendIRCode("samsung_ac", "power");
        std::this_thread::sleep_for(std::chrono::milliseconds(500));
        ir.sendIRCode("samsung_ac", "temp_up");
        std::this_thread::sleep_for(std::chrono::milliseconds(500));
        ir.sendIRCode("samsung_ac", "temp_down");
        
        // MQTT 상태 발행
        mqtt.publish("irremote/status", "test_sequence_completed");
        
        std::cout << "\n✅ 테스트 시퀀스 완료" << std::endl;
    }
};

int main() {
    std::cout << "🏠 IR Remote Controller - Windows 시뮬레이션" << std::endl;
    std::cout << "===========================================" << std::endl;
    
    IRRemoteSimulator simulator;
    
    try {
        simulator.initialize();
        simulator.runSimulation();
    } catch (const std::exception& e) {
        std::cout << "❌ 오류 발생: " << e.what() << std::endl;
        return 1;
    }
    
    return 0;
}
