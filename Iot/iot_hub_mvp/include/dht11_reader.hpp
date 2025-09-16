#pragma once
#include <optional>   // 추가!
struct Dht11Data { float tempC{}; float hum{}; };

class Dht11Reader {
public:
    explicit Dht11Reader(int bcmPin): pin_(bcmPin) {}
    bool init();
    std::optional<Dht11Data> read_once(int timeout_ms=1500);
private:
    int pin_;
};
