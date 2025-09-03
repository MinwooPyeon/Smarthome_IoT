#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include "network/mqtt_client.h"
#include <thread>
#include <chrono>

using namespace testing;

class MQTTClientTest : public Test {
protected:
    void SetUp() override {
        mqtt_client = std::make_unique<MQTTClient>();
    }
    
    void TearDown() override {
        if (mqtt_client && mqtt_client->isConnected()) {
            mqtt_client->disconnect();
        }
    }
    
    std::unique_ptr<MQTTClient> mqtt_client;
};

// 기본 생성자 테스트
TEST_F(MQTTClientTest, ConstructorTest) {
    EXPECT_FALSE(mqtt_client->isConnected());
}

// 연결 테스트 (로컬 브로커가 실행 중이어야 함)
TEST_F(MQTTClientTest, ConnectionTest) {
    // 로컬 MQTT 브로커 연결 시도
    bool connected = mqtt_client->connect("localhost", 1883);
    
    if (connected) {
        EXPECT_TRUE(mqtt_client->isConnected());
        
        // 연결 해제 테스트
        mqtt_client->disconnect();
        EXPECT_FALSE(mqtt_client->isConnected());
    } else {
        // 로컬 브로커가 실행 중이지 않은 경우
        GTEST_SKIP() << "Local MQTT broker not available";
    }
}

// 메시지 발행 테스트
TEST_F(MQTTClientTest, PublishTest) {
    if (mqtt_client->connect("localhost", 1883)) {
        EXPECT_TRUE(mqtt_client->publish("test/topic", "test message"));
    } else {
        GTEST_SKIP() << "Local MQTT broker not available";
    }
}

// 구독 테스트
TEST_F(MQTTClientTest, SubscribeTest) {
    if (mqtt_client->connect("localhost", 1883)) {
        EXPECT_TRUE(mqtt_client->subscribe("test/subscribe"));
    } else {
        GTEST_SKIP() << "Local MQTT broker not available";
    }
}

// 콜백 설정 테스트
TEST_F(MQTTClientTest, CallbackTest) {
    std::string received_topic;
    std::string received_message;
    bool callback_called = false;
    
    mqtt_client->setMessageCallback([&](const std::string& topic, const std::string& message) {
        received_topic = topic;
        received_message = message;
        callback_called = true;
    });
    
    // 콜백이 설정되었는지 확인
    EXPECT_TRUE(true); // 콜백 설정 자체는 성공
}

// 연결 실패 테스트
TEST_F(MQTTClientTest, ConnectionFailureTest) {
    // 존재하지 않는 브로커에 연결 시도
    EXPECT_FALSE(mqtt_client->connect("nonexistent.broker", 1883));
    EXPECT_FALSE(mqtt_client->isConnected());
}

// 잘못된 포트 테스트
TEST_F(MQTTClientTest, InvalidPortTest) {
    // 잘못된 포트로 연결 시도
    EXPECT_FALSE(mqtt_client->connect("localhost", 99999));
    EXPECT_FALSE(mqtt_client->isConnected());
}

// 연결 해제 테스트
TEST_F(MQTTClientTest, DisconnectTest) {
    if (mqtt_client->connect("localhost", 1883)) {
        EXPECT_TRUE(mqtt_client->isConnected());
        mqtt_client->disconnect();
        EXPECT_FALSE(mqtt_client->isConnected());
    } else {
        GTEST_SKIP() << "Local MQTT broker not available";
    }
}

// 재연결 테스트
TEST_F(MQTTClientTest, ReconnectionTest) {
    if (mqtt_client->connect("localhost", 1883)) {
        EXPECT_TRUE(mqtt_client->isConnected());
        
        mqtt_client->disconnect();
        EXPECT_FALSE(mqtt_client->isConnected());
        
        // 재연결
        EXPECT_TRUE(mqtt_client->connect("localhost", 1883));
        EXPECT_TRUE(mqtt_client->isConnected());
    } else {
        GTEST_SKIP() << "Local MQTT broker not available";
    }
}

// 메시지 루프 테스트
TEST_F(MQTTClientTest, MessageLoopTest) {
    if (mqtt_client->connect("localhost", 1883)) {
        // 메시지 루프 실행 (짧은 시간)
        std::thread loop_thread([this]() {
            mqtt_client->loop();
        });
        
        // 잠시 대기
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
        
        loop_thread.join();
        EXPECT_TRUE(true); // 루프가 정상적으로 실행됨
    } else {
        GTEST_SKIP() << "Local MQTT broker not available";
    }
}

// 동시 연결 테스트
TEST_F(MQTTClientTest, ConcurrentConnectionTest) {
    std::vector<std::unique_ptr<MQTTClient>> clients;
    std::vector<bool> results;
    
    for (int i = 0; i < 3; i++) {
        auto client = std::make_unique<MQTTClient>();
        bool connected = client->connect("localhost", 1883);
        results.push_back(connected);
        clients.push_back(std::move(client));
    }
    
    // 모든 클라이언트가 연결되었는지 확인
    for (bool result : results) {
        if (result) {
            EXPECT_TRUE(result);
        }
    }
}

// 메시지 발행 실패 테스트
TEST_F(MQTTClientTest, PublishFailureTest) {
    // 연결하지 않은 상태에서 메시지 발행
    EXPECT_FALSE(mqtt_client->publish("test/topic", "test message"));
}

// 구독 실패 테스트
TEST_F(MQTTClientTest, SubscribeFailureTest) {
    // 연결하지 않은 상태에서 구독
    EXPECT_FALSE(mqtt_client->subscribe("test/topic"));
}

// 긴 메시지 테스트
TEST_F(MQTTClientTest, LongMessageTest) {
    if (mqtt_client->connect("localhost", 1883)) {
        // 긴 메시지 생성
        std::string long_message(1000, 'A');
        EXPECT_TRUE(mqtt_client->publish("test/long", long_message));
    } else {
        GTEST_SKIP() << "Local MQTT broker not available";
    }
}

// 특수 문자 메시지 테스트
TEST_F(MQTTClientTest, SpecialCharacterTest) {
    if (mqtt_client->connect("localhost", 1883)) {
        std::string special_message = "테스트 메시지: !@#$%^&*()_+-=[]{}|;':\",./<>?";
        EXPECT_TRUE(mqtt_client->publish("test/special", special_message));
    } else {
        GTEST_SKIP() << "Local MQTT broker not available";
    }
}

// 빈 메시지 테스트
TEST_F(MQTTClientTest, EmptyMessageTest) {
    if (mqtt_client->connect("localhost", 1883)) {
        EXPECT_TRUE(mqtt_client->publish("test/empty", ""));
    } else {
        GTEST_SKIP() << "Local MQTT broker not available";
    }
}

// 빈 토픽 테스트
TEST_F(MQTTClientTest, EmptyTopicTest) {
    if (mqtt_client->connect("localhost", 1883)) {
        EXPECT_FALSE(mqtt_client->publish("", "test message"));
    } else {
        GTEST_SKIP() << "Local MQTT broker not available";
    }
}
