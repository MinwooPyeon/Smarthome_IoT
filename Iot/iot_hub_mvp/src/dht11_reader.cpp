#include "dht11_reader.hpp"
#include <pigpio.h>
#include <thread>
#include <vector>
#include <chrono>

// 간단 DHT11 비트뽑기: pigpio로 엄격 타이밍
// 참고: DHT11은 18ms Low -> High -> 읽기. 여기선 사용자 공간 근사치.

bool Dht11Reader::init(){
    return gpioSetMode(pin_, PI_OUTPUT) == 0;
}

std::optional<Dht11Data> Dht11Reader::read_once(int timeout_ms){
    using namespace std::chrono;
    auto deadline = steady_clock::now() + milliseconds(timeout_ms);

    // 1) Start signal
    gpioSetMode(pin_, PI_OUTPUT);
    gpioWrite(pin_, 0);
    gpioDelay(18000); // 18ms
    gpioWrite(pin_, 1);
    gpioDelay(40);
    gpioSetMode(pin_, PI_INPUT);

    // 2) 응답 파형 캡처
    // 80us Low, 80us High 후 40bit(각 bit: 50us Low + 26~28us High=0, 70us High=1)
    // 여기서는 busy-wait로 간단히 측정(실환경은 에러 보정 필요)
    auto wait_level = [&](int level, int max_us)->int{
        int t=0;
        while(gpioRead(pin_)!=level){
            gpioDelay(1); if(++t>max_us) return -1;
            if(steady_clock::now() > deadline) return -1;
        }
        return t;
    };

    if(wait_level(0, 100)==-1) return std::nullopt; // 응답 Low
    if(wait_level(1, 200)==-1) return std::nullopt; // 응답 High

    std::vector<int> bits;
    bits.reserve(40);
    for(int i=0;i<40;i++){
        if(wait_level(0, 100)==-1) return std::nullopt; // 50us Low
        int t=0;
        while(gpioRead(pin_)==1){
            gpioDelay(1); t++;
            if(t>150 || steady_clock::now()>deadline) return std::nullopt;
        }
        // t(High 길이 us 유사치)로 0/1 판정
        bits.push_back(t>50 ? 1 : 0); // 약식 임계
    }

    // 5 바이트 조립
    int data[5]={0,};
    for(int i=0;i<40;i++){
        data[i/8] <<= 1;
        data[i/8] |= bits[i];
    }
    int sum = (data[0]+data[1]+data[2]+data[3]) & 0xFF;
    if(sum != data[4]) return std::nullopt;

    Dht11Data out;
    out.hum   = (float)data[0];
    out.tempC = (float)data[2];
    return out;
}
