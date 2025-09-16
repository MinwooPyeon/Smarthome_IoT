#pragma once
#include <vector>
#include <optional>  // 추가!

struct IrFrame { std::vector<int> raw_us; int gapUs; };

class IrReceiver {
public:
    IrReceiver(int bcmPin, int gapUs): pin_(bcmPin), gapUs_(gapUs) {}
    bool init(int glitchFilterUs=50);
    std::optional<IrFrame> capture_once(int timeout_ms=1500);
private:
    int pin_;
    int gapUs_;
};
