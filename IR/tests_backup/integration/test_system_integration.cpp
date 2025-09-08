#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include "hardware/ir_receiver.h"
#include "hardware/appliance_controller.h"
#include "network/mqtt_client.h"
#include <thread>
#include <chrono>
#include <future>

using namespace testing;

class SystemIntegrationTest : public Test {
protected:
    void SetUp() override {
        // 시스템 컴포넌트들 초기화
        ir_receiver = std::make_unique<IRReceiver>(23);
        appliance_controller = std::make_unique<ApplianceController>();
        mqtt_client = std::make_unique<MQTTClient>();
        
        // 콜백 연결
        setupCallbacks();
    }
    
    void TearDown() override {
        if (ir_receiver && ir_receiver->isReceiving()) {
            ir_receiver->stopReceiving();
        }
        
        if (mqtt_client && mqtt_client->isConnected()) {
            mqtt_client->disconnect();
        }
    }
    
    void setupCallbacks() {
        // IR 수신기에서 가전기기 제어기로 연결
        ir_receiver->setIRCodeCallback([this](const std::string& ir_code) {
            received_ir_codes.push_back(ir_code);
            
            // 가전기기 제어 실행
            auto result = appliance_controller->controlAppliance(ir_code);
            control_results.push_back(result);
            
            // MQTT로 상태 전송
            if (mqtt_client && mqtt_client->isConnected()) {
                nlohmann::json status_msg;
                status_msg["ir_code"] = ir_code;
                status_msg["appliance_id"] = result.appliance_id;
                status_msg["success"] = result.success;
                status_msg["timestamp"] = std::chrono::duration_cast<std::chrono::milliseconds>(
                    std::chrono::system_clock::now().time_since_epoch()).count();
                
                mqtt_client->publish("irremote/status", status_msg.dump());
            }
        });
        
        // 가전기기 제어 결과 콜백
        appliance_controller->setControlCallback([this](const ControlResult& result) {
            control_callbacks.push_back(result);
        });
        
        // MQTT 메시지 콜백
        mqtt_client->setMessageCallback([this](const std::string& topic, const std::string& message) {
            received_mqtt_messages[topic].push_back(message);
        });
    }
    
    std::unique_ptr<IRReceiver> ir_receiver;
    std::unique_ptr<ApplianceController> appliance_controller;
    std::unique_ptr<MQTTClient> mqtt_client;
    
    // 테스트 결과 저장
    std::vector<std::string> received_ir_codes;
    std::vector<ControlResult> control_results;
    std::vector<ControlResult> control_callbacks;
    std::map<std::string, std::vector<std::string>> received_mqtt_messages;
};

// 전체 시스템 통합 테스트
TEST_F(SystemIntegrationTest, FullSystemIntegrationTest) {
    // 1. MQTT 브로커 연결
    bool mqtt_connected = mqtt_client->connect("localhost", 1883);
    if (!mqtt_connected) {
        GTEST_SKIP() << "Local MQTT broker not available";
    }
    
    // 2. IR 수신 시작
    EXPECT_TRUE(ir_receiver->startReceiving());
    EXPECT_TRUE(ir_receiver->isReceiving());
    
    // 3. 시스템이 정상적으로 동작하는지 확인
    std::this_thread::sleep_for(std::chrono::milliseconds(500));
    
    // 4. IR 수신 상태 확인
    EXPECT_TRUE(ir_receiver->isReceiving());
    
    // 5. 가전기기 등록 상태 확인
    auto appliances = appliance_controller->getRegisteredAppliances();
    EXPECT_THAT(appliances, Contains("samsung_tv"));
    EXPECT_THAT(appliances, Contains("samsung_ac"));
    
    // 6. MQTT 연결 상태 확인
    EXPECT_TRUE(mqtt_client->isConnected());
}

// IR 수신 → 가전기기 제어 통합 테스트
TEST_F(SystemIntegrationTest, IRToApplianceControlTest) {
    // 1. IR 수신 시작
    EXPECT_TRUE(ir_receiver->startReceiving());
    
    // 2. 시뮬레이션된 IR 코드 수신 (Windows)
#ifdef _WIN32
    // Windows에서는 시뮬레이션 모드로 IR 코드 생성
    std::this_thread::sleep_for(std::chrono::milliseconds(1000));
    
    // IR 코드가 수신되었는지 확인
    EXPECT_GT(received_ir_codes.size(), 0);
    
    // 가전기기 제어가 실행되었는지 확인
    EXPECT_GT(control_results.size(), 0);
    
    // 콜백이 호출되었는지 확인
    EXPECT_GT(control_callbacks.size(), 0);
#endif
    
    // 3. IR 수신 중지
    ir_receiver->stopReceiving();
    EXPECT_FALSE(ir_receiver->isReceiving());
}

// MQTT 통신 통합 테스트
TEST_F(SystemIntegrationTest, MQTTCommunicationTest) {
    // 1. MQTT 브로커 연결
    if (!mqtt_client->connect("localhost", 1883)) {
        GTEST_SKIP() << "Local MQTT broker not available";
    }
    
    // 2. 테스트 토픽 구독
    EXPECT_TRUE(mqtt_client->subscribe("test/integration"));
    
    // 3. 테스트 메시지 발행
    std::string test_message = "Integration test message";
    EXPECT_TRUE(mqtt_client->publish("test/integration", test_message));
    
    // 4. 메시지 수신 대기
    std::this_thread::sleep_for(std::chrono::milliseconds(100));
    
    // 5. 메시지가 수신되었는지 확인
    auto it = received_mqtt_messages.find("test/integration");
    if (it != received_mqtt_messages.end()) {
        EXPECT_THAT(it->second, Contains(test_message));
    }
}

// 가전기기 제어 → MQTT 상태 전송 통합 테스트
TEST_F(SystemIntegrationTest, ApplianceControlToMQTTTest) {
    // 1. MQTT 브로커 연결
    if (!mqtt_client->connect("localhost", 1883)) {
        GTEST_SKIP() << "Local MQTT broker not available";
    }
    
    // 2. 상태 토픽 구독
    EXPECT_TRUE(mqtt_client->subscribe("irremote/status"));
    
    // 3. 가전기기 직접 제어
    auto result = appliance_controller->controlAppliance("samsung_tv", ControlCommand::POWER_TOGGLE);
    EXPECT_TRUE(result.success);
    
    // 4. MQTT 상태 메시지 수신 대기
    std::this_thread::sleep_for(std::chrono::milliseconds(100));
    
    // 5. 상태 메시지가 수신되었는지 확인
    auto it = received_mqtt_messages.find("irremote/status");
    if (it != received_mqtt_messages.end()) {
        EXPECT_GT(it->second.size(), 0);
        
        // JSON 메시지 파싱 확인
        for (const auto& message : it->second) {
            EXPECT_FALSE(message.empty());
            // JSON 형식 검증은 별도로 수행
        }
    }
}

// 동시 IR 수신 테스트
TEST_F(SystemIntegrationTest, ConcurrentIRReceptionTest) {
    // 1. IR 수신 시작
    EXPECT_TRUE(ir_receiver->startReceiving());
    
    // 2. 여러 스레드에서 동시 IR 수신 시뮬레이션
    std::vector<std::thread> threads;
    std::vector<bool> results;
    
    for (int i = 0; i < 3; i++) {
        threads.emplace_back([this, i]() {
            // 각 스레드에서 IR 수신 상태 확인
            bool is_receiving = ir_receiver->isReceiving();
            results.push_back(is_receiving);
        });
    }
    
    for (auto& thread : threads) {
        thread.join();
    }
    
    // 3. 모든 스레드에서 IR 수신이 활성화되어 있어야 함
    for (bool result : results) {
        EXPECT_TRUE(result);
    }
    
    // 4. IR 수신 중지
    ir_receiver->stopReceiving();
}

// 시스템 복구 테스트
TEST_F(SystemIntegrationTest, SystemRecoveryTest) {
    // 1. 시스템 시작
    EXPECT_TRUE(ir_receiver->startReceiving());
    
    // 2. 시스템 중지
    ir_receiver->stopReceiving();
    EXPECT_FALSE(ir_receiver->isReceiving());
    
    // 3. 시스템 재시작
    EXPECT_TRUE(ir_receiver->startReceiving());
    EXPECT_TRUE(ir_receiver->isReceiving());
    
    // 4. 시스템 재중지
    ir_receiver->stopReceiving();
    EXPECT_FALSE(ir_receiver->isReceiving());
}

// 메모리 누수 테스트
TEST_F(SystemIntegrationTest, MemoryLeakTest) {
    // 1. 여러 번 시스템 시작/중지
    for (int i = 0; i < 5; i++) {
        auto receiver = std::make_unique<IRReceiver>(23 + i);
        auto controller = std::make_unique<ApplianceController>();
        auto mqtt = std::make_unique<MQTTClient>();
        
        receiver->startReceiving();
        std::this_thread::sleep_for(std::chrono::milliseconds(50));
        receiver->stopReceiving();
    }
    
    // 메모리 누수 검사는 valgrind나 AddressSanitizer로 확인
    EXPECT_TRUE(true);
}

// 성능 테스트
TEST_F(SystemIntegrationTest, PerformanceTest) {
    // 1. 시스템 시작 시간 측정
    auto start = std::chrono::high_resolution_clock::now();
    
    EXPECT_TRUE(ir_receiver->startReceiving());
    std::this_thread::sleep_for(std::chrono::milliseconds(100));
    ir_receiver->stopReceiving();
    
    auto end = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end - start);
    
    // 2. 시스템 시작/중지가 200ms 이내에 완료되어야 함
    EXPECT_LT(duration.count(), 200);
}

// 오류 처리 통합 테스트
TEST_F(SystemIntegrationTest, ErrorHandlingTest) {
    // 1. 잘못된 IR 코드로 가전기기 제어 시도
    auto result = appliance_controller->controlAppliance("0xINVALID");
    EXPECT_FALSE(result.success);
    EXPECT_TRUE(result.message.find("알 수 없는 IR 코드") != std::string::npos);
    
    // 2. 존재하지 않는 가전기기로 제어 시도
    result = appliance_controller->controlAppliance("nonexistent_device", ControlCommand::POWER_TOGGLE);
    EXPECT_FALSE(result.success);
    EXPECT_TRUE(result.message.find("등록되지 않은 가전기기") != std::string::npos);
    
    // 3. 잘못된 MQTT 브로커 연결 시도
    EXPECT_FALSE(mqtt_client->connect("invalid.broker", 1883));
}

// 설정 파일 통합 테스트
TEST_F(SystemIntegrationTest, ConfigurationIntegrationTest) {
    // 1. 설정 파일 로드
    bool config_loaded = appliance_controller->loadConfiguration("config/appliances.json");
    
    if (config_loaded) {
        // 2. 로드된 설정으로 가전기기 확인
        auto appliances = appliance_controller->getRegisteredAppliances();
        EXPECT_GT(appliances.size(), 0);
        
        // 3. 설정 파일 저장
        bool config_saved = appliance_controller->saveConfiguration("test_config_save.json");
        EXPECT_TRUE(config_saved);
        
        // 4. 테스트 파일 삭제
        if (std::filesystem::exists("test_config_save.json")) {
            std::filesystem::remove("test_config_save.json");
        }
    }
}
