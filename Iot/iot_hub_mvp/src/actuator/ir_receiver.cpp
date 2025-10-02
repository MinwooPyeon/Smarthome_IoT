#include "actuator/ir_receiver.hpp"
#include <pigpio.h>
#include <chrono>
#include <thread>

bool IrReceiver::init(int glitchUs){
    if(gpioSetMode(pin_, PI_INPUT)!=0) return false;
    if(gpioGlitchFilter(pin_, glitchUs)!=0) return false;
    return true;
}

std::optional<IrSample> IrReceiver::capture_once(int timeout_ms){
    using namespace std::chrono;
    auto deadline = steady_clock::now() + milliseconds(timeout_ms);

    // 에지 폴링(Simple): 실제는 callback으로 더 안정적
    int last = gpioRead(pin_);
    uint32_t t0 = gpioTick();
    std::vector<int> seq;
    while(steady_clock::now() < deadline){
        int v = gpioRead(pin_);
        if(v != last){
            uint32_t t1 = gpioTick();
            int dt = (int)(t1 - t0); // us
            seq.push_back(dt);       // 이전 상태 지속시간
            t0 = t1;
            last = v;

            // 프레임 분리(긴 무신호=gap)
            if(dt >= gapUs_ && !seq.empty()){
                // 마지막 gap은 포함하지 않도록 제거
                if(!seq.empty()) seq.pop_back();
                if(seq.size()>=4){
                    IrSample f; f.rawUs = std::move(seq); f.gapUs = gapUs_;
                    return f;
                }else{
                    seq.clear(); // 노이즈 무시
                }
            }
        }
        // 작은 대기
        gpioDelay(50);
    }
    return std::nullopt; // timeout
}
