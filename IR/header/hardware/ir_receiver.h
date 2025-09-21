#ifndef IR_RECEIVER_H
#define IR_RECEIVER_H

#include <string>
#include <functional>
#include <thread>
#include <atomic>

#ifdef ESP32
#include "Arduino.h"
#endif

class IRReceiver {
public:
    IRReceiver(int gpio_pin = 23);
    ~IRReceiver();

    bool startReceiving();
    void stopReceiving();
    bool isReceiving() const;

    std::string receiveIRCode();

    void setIRCodeCallback(std::function<void(const std::string&)> callback);

    void setGPIO(int gpio_pin);
    int getGPIO() const;

private:
    int gpio_pin_;
    std::atomic<bool> is_receiving_;
    std::thread receive_thread_;
    std::function<void(const std::string&)> ir_code_callback_;

#ifdef ESP32
    bool is_initialized_;
#endif

    void receiveLoop();

    std::string readIRCode();

    std::string decodeNECProtocol();

    std::string decodeRC5Protocol();

    std::string decodeSonyProtocol();

    std::string getProtocolName(int protocol);
};

#endif
