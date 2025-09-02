#ifndef IR_RECEIVER_H
#define IR_RECEIVER_H

#include <string>
#include <functional>
#include <thread>
#include <atomic>

class IRReceiver {
public:
    IRReceiver(int gpio_pin = 23);
    ~IRReceiver();
    
    // IR 수신 시작/중지
    bool startReceiving();
    void stopReceiving();
    bool isReceiving() const;
    
    // IR 코드 수신
    std::string receiveIRCode();
    
    // 콜백 설정
    void setIRCodeCallback(std::function<void(const std::string&)> callback);
    
    // GPIO 핀 설정
    void setGPIO(int gpio_pin);
    int getGPIO() const;

private:
    int gpio_pin_;
    std::atomic<bool> is_receiving_;
    std::thread receive_thread_;
    std::function<void(const std::string&)> ir_code_callback_;
    
    // 수신 스레드 함수
    void receiveLoop();
    
    // IR 코드 읽기
    std::string readIRCode();
    
    // NEC 프로토콜 디코딩
    std::string decodeNECProtocol();
    
    // RC5 프로토콜 디코딩
    std::string decodeRC5Protocol();
    
    // Sony 프로토콜 디코딩
    std::string decodeSonyProtocol();
};

#endif // IR_RECEIVER_H
