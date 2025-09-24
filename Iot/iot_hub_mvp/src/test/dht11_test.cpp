#include "dht11_reader.hpp"
#include <pigpio.h>
#include <iostream>
#include <cstdint>

int main(int argc, char** argv) {
    if (gpioInitialise() < 0) { std::cerr << "pigpio init failed\n"; return 1; }

    int pin = (argc > 1) ? std::stoi(argv[1]) : 4;
    Dht11Reader dht(pin);
    dht.init();

    // 5회까지 시도, 각 시도 1.2초 간격
    auto r = dht.read_with_retry(5, 2000, 1200);
    if (r) {
        std::cout << "OK: T=" << r->tempC << "C  H=" << r->hum << "%\n";
    } else {
        std::cout << "DHT11 read failed after retries\n";
    }

    gpioTerminate();
    return 0;
}
