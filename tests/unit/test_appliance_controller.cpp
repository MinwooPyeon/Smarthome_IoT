#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include "hardware/appliance_controller.h"
#include <fstream>
#include <filesystem>

using namespace testing;

class ApplianceControllerTest : public Test {
protected:
    void SetUp() override {
        controller = std::make_unique<ApplianceController>();
        
        // 테스트용 설정 파일 생성
        createTestConfigFile();
    }
    
    void TearDown() override {
        // 테스트용 설정 파일 삭제
        if (std::filesystem::exists("test_appliances.json")) {
            std::filesystem::remove("test_appliances.json");
        }
    }
    
    void createTestConfigFile() {
        std::ofstream file("test_appliances.json");
        file << R"({
            "appliances": [
                {
                    "id": "test_tv",
                    "type": "TV",
                    "gpio_pin": 24
                },
                {
                    "id": "test_ac",
                    "type": "AIR_CONDITIONER",
                    "gpio_pin": 25
                }
            ]
        })";
        file.close();
    }
    
    std::unique_ptr<ApplianceController> controller;
};

// 기본 생성자 테스트
TEST_F(ApplianceControllerTest, ConstructorTest) {
    auto appliances = controller->getRegisteredAppliances();
    
    // 기본 가전기기들이 등록되어 있어야 함
    EXPECT_THAT(appliances, Contains("samsung_tv"));
    EXPECT_THAT(appliances, Contains("samsung_ac"));
    EXPECT_THAT(appliances, Contains("samsung_purifier"));
    EXPECT_THAT(appliances, Contains("general_projector"));
}

// 가전기기 등록/해제 테스트
TEST_F(ApplianceControllerTest, RegisterUnregisterTest) {
    EXPECT_TRUE(controller->registerAppliance("test_device", ApplianceType::TV));
    
    auto appliances = controller->getRegisteredAppliances();
    EXPECT_THAT(appliances, Contains("test_device"));
    
    EXPECT_TRUE(controller->unregisterAppliance("test_device"));
    
    appliances = controller->getRegisteredAppliances();
    EXPECT_THAT(appliances, Not(Contains("test_device")));
}

// 가전기기 타입 조회 테스트
TEST_F(ApplianceControllerTest, GetApplianceTypeTest) {
    EXPECT_EQ(controller->getApplianceType("samsung_tv"), ApplianceType::TV);
    EXPECT_EQ(controller->getApplianceType("samsung_ac"), ApplianceType::AIR_CONDITIONER);
    EXPECT_EQ(controller->getApplianceType("samsung_purifier"), ApplianceType::AIR_PURIFIER);
    EXPECT_EQ(controller->getApplianceType("general_projector"), ApplianceType::PROJECTOR);
    EXPECT_EQ(controller->getApplianceType("unknown_device"), ApplianceType::UNKNOWN);
}

// IR 코드 변환 테스트
TEST_F(ApplianceControllerTest, IRCodeConversionTest) {
    // Samsung TV 전원 코드
    auto command = controller->convertIRToCommand("0xE0E040BF");
    EXPECT_EQ(command, ControlCommand::POWER_TOGGLE);
    
    // Samsung AC 온도 설정 코드
    command = controller->convertIRToCommand("0xE0E018E7");
    EXPECT_EQ(command, ControlCommand::TEMP_SET);
    
    // 알 수 없는 코드
    command = controller->convertIRToCommand("0x12345678");
    EXPECT_EQ(command, ControlCommand::UNKNOWN);
}

// 가전기기 ID 조회 테스트
TEST_F(ApplianceControllerTest, GetApplianceIdTest) {
    // Samsung TV 전원 코드
    auto appliance_id = controller->getApplianceId("0xE0E040BF");
    EXPECT_EQ(appliance_id, "samsung_tv");
    
    // Samsung AC 온도 설정 코드
    appliance_id = controller->getApplianceId("0xE0E018E7");
    EXPECT_EQ(appliance_id, "samsung_ac");
    
    // 알 수 없는 코드
    appliance_id = controller->getApplianceId("0x12345678");
    EXPECT_TRUE(appliance_id.empty());
}

// IR 코드로 가전기기 제어 테스트
TEST_F(ApplianceControllerTest, ControlApplianceByIRCodeTest) {
    // Samsung TV 전원 코드로 제어
    auto result = controller->controlAppliance("0xE0E040BF");
    EXPECT_TRUE(result.success);
    EXPECT_EQ(result.appliance_id, "samsung_tv");
    EXPECT_EQ(result.command, ControlCommand::POWER_TOGGLE);
    
    // 알 수 없는 IR 코드로 제어
    result = controller->controlAppliance("0x12345678");
    EXPECT_FALSE(result.success);
    EXPECT_TRUE(result.message.find("알 수 없는 IR 코드") != std::string::npos);
}

// 직접 가전기기 제어 테스트
TEST_F(ApplianceControllerTest, DirectControlTest) {
    // 등록된 가전기기로 제어
    auto result = controller->controlAppliance("samsung_tv", ControlCommand::POWER_TOGGLE);
    EXPECT_TRUE(result.success);
    
    // 등록되지 않은 가전기기로 제어
    result = controller->controlAppliance("unknown_device", ControlCommand::POWER_TOGGLE);
    EXPECT_FALSE(result.success);
    EXPECT_TRUE(result.message.find("등록되지 않은 가전기기") != std::string::npos);
}

// 콜백 설정 테스트
TEST_F(ApplianceControllerTest, CallbackTest) {
    ControlResult received_result;
    bool callback_called = false;
    
    controller->setControlCallback([&](const ControlResult& result) {
        received_result = result;
        callback_called = true;
    });
    
    // 제어 실행으로 콜백 호출
    controller->controlAppliance("samsung_tv", ControlCommand::POWER_TOGGLE);
    
    EXPECT_TRUE(callback_called);
    EXPECT_EQ(received_result.appliance_id, "samsung_tv");
    EXPECT_EQ(received_result.command, ControlCommand::POWER_TOGGLE);
}

// 설정 파일 로드 테스트
TEST_F(ApplianceControllerTest, LoadConfigurationTest) {
    EXPECT_TRUE(controller->loadConfiguration("test_appliances.json"));
    
    auto appliances = controller->getRegisteredAppliances();
    EXPECT_THAT(appliances, Contains("test_tv"));
    EXPECT_THAT(appliances, Contains("test_ac"));
}

// 설정 파일 저장 테스트
TEST_F(ApplianceControllerTest, SaveConfigurationTest) {
    EXPECT_TRUE(controller->saveConfiguration("test_save.json"));
    
    // 저장된 파일이 존재하는지 확인
    EXPECT_TRUE(std::filesystem::exists("test_save.json"));
    
    // 테스트 후 파일 삭제
    std::filesystem::remove("test_save.json");
}

// 잘못된 설정 파일 처리 테스트
TEST_F(ApplianceControllerTest, InvalidConfigurationTest) {
    // 존재하지 않는 파일 로드
    EXPECT_FALSE(controller->loadConfiguration("nonexistent.json"));
    
    // 잘못된 JSON 형식 파일 생성
    std::ofstream invalid_file("invalid.json");
    invalid_file << "{ invalid json }";
    invalid_file.close();
    
    EXPECT_FALSE(controller->loadConfiguration("invalid.json"));
    
    // 테스트 후 파일 삭제
    std::filesystem::remove("invalid.json");
}

// GPIO 제어 테스트 (Windows 시뮬레이션)
#ifdef _WIN32
TEST_F(ApplianceControllerTest, GPIOControlTest) {
    // Windows에서는 시뮬레이션 모드
    auto result = controller->controlAppliance("samsung_tv", ControlCommand::POWER_TOGGLE);
    EXPECT_TRUE(result.success);
}
#endif

// 동시 제어 테스트
TEST_F(ApplianceControllerTest, ConcurrentControlTest) {
    std::vector<std::thread> threads;
    std::vector<bool> results(5);
    
    for (int i = 0; i < 5; i++) {
        threads.emplace_back([&, i]() {
            auto result = controller->controlAppliance("samsung_tv", ControlCommand::POWER_TOGGLE);
            results[i] = result.success;
        });
    }
    
    for (auto& thread : threads) {
        thread.join();
    }
    
    // 모든 스레드에서 성공해야 함
    for (bool result : results) {
        EXPECT_TRUE(result);
    }
}
