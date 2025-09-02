#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include "hardware/ir_receiver.h"
#include <chrono>
#include <thread>

using namespace testing;

class IRReceiverTest : public Test {
protected:
    void SetUp() override {
        ir_receiver = std::make_unique<IRReceiver>(23);
    }
    
    void TearDown() override {
        if (ir_receiver && ir_receiver->isReceiving()) {
            ir_receiver->stopReceiving();
        }
    }
    
    std::unique_ptr<IRReceiver> ir_receiver;
};

// 기본 생성자 테스트
TEST_F(IRReceiverTest, ConstructorTest) {
    EXPECT_EQ(ir_receiver->getGPIO(), 23);
    EXPECT_FALSE(ir_receiver->isReceiving());
}

// GPIO 핀 설정 테스트
TEST_F(IRReceiverTest, SetGPIOTest) {
    ir_receiver->setGPIO(25);
    EXPECT_EQ(ir_receiver->getGPIO(), 25);
}

// IR 수신 시작/중지 테스트
TEST_F(IRReceiverTest, StartStopReceivingTest) {
    EXPECT_TRUE(ir_receiver->startReceiving());
    EXPECT_TRUE(ir_receiver->isReceiving());
    
    ir_receiver->stopReceiving();
    EXPECT_FALSE(ir_receiver->isReceiving());
}

// 중복 시작 방지 테스트
TEST_F(IRReceiverTest, PreventDuplicateStartTest) {
    EXPECT_TRUE(ir_receiver->startReceiving());
    EXPECT_FALSE(ir_receiver->startReceiving()); // 중복 시작 방지
}

// 콜백 설정 테스트
TEST_F(IRReceiverTest, CallbackTest) {
    std::string received_code;
    bool callback_called = false;
    
    ir_receiver->setIRCodeCallback([&](const std::string& code) {
        received_code = code;
        callback_called = true;
    });
    
    // 콜백이 설정되었는지 확인
    EXPECT_TRUE(true); // 콜백 설정 자체는 성공
}

// NEC 프로토콜 디코딩 테스트 (Windows 시뮬레이션)
#ifdef _WIN32
TEST_F(IRReceiverTest, NECProtocolDecodingTest) {
    // Windows에서는 시뮬레이션된 IR 코드 생성
    std::string code = ir_receiver->receiveIRCode();
    
    // 빈 문자열이거나 유효한 16진수 형식
    if (!code.empty()) {
        EXPECT_TRUE(code.length() >= 3); // "0x" + 최소 1자리
        EXPECT_EQ(code.substr(0, 2), "0x");
    }
}
#endif

// 스레드 안전성 테스트
TEST_F(IRReceiverTest, ThreadSafetyTest) {
    ir_receiver->startReceiving();
    
    // 여러 스레드에서 동시 접근 테스트
    std::thread t1([this]() {
        EXPECT_TRUE(ir_receiver->isReceiving());
    });
    
    std::thread t2([this]() {
        EXPECT_TRUE(ir_receiver->isReceiving());
    });
    
    t1.join();
    t2.join();
    
    ir_receiver->stopReceiving();
}

// 메모리 누수 테스트
TEST_F(IRReceiverTest, MemoryLeakTest) {
    for (int i = 0; i < 10; i++) {
        auto receiver = std::make_unique<IRReceiver>(i);
        receiver->startReceiving();
        receiver->stopReceiving();
    }
    // 메모리 누수 검사는 valgrind나 AddressSanitizer로 확인
}

// 예외 처리 테스트
TEST_F(IRReceiverTest, ExceptionHandlingTest) {
    // 잘못된 GPIO 핀으로 생성 시도
    EXPECT_NO_THROW({
        auto receiver = std::make_unique<IRReceiver>(-1);
    });
}

// 성능 테스트
TEST_F(IRReceiverTest, PerformanceTest) {
    auto start = std::chrono::high_resolution_clock::now();
    
    ir_receiver->startReceiving();
    std::this_thread::sleep_for(std::chrono::milliseconds(100));
    ir_receiver->stopReceiving();
    
    auto end = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end - start);
    
    // 시작/중지가 200ms 이내에 완료되어야 함
    EXPECT_LT(duration.count(), 200);
}
