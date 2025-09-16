#include "dht11_reader.hpp"
#include <pigpio.h>
#include <iostream>

int main(int argc, char** argv) {
    if (gpioInitialise() < 0) {
        std::cerr << "pigpio init failed\n";
        return 1;
    }

    int pin = 4; // BCM 핀 (기본값)
    if (argc > 1) pin = std::stoi(argv[1]);

    Dht11Reader dht(pin);
    if (!dht.init()) {
        std::cerr << "dht11 init failed on pin " << pin << "\n";
        gpioTerminate();
        return 1;
    }

    auto res = dht.read_once(2000); // timeout 2초
    if (res) {
        std::cout << "DHT11(" << pin << ") "
                  << "Temp=" << res->tempC
                  << "C  Hum=" << res->hum << "%\n";
    } else {
        std::cout << "DHT11 read failed (timeout or checksum error)\n";
    }

    gpioTerminate();
    return 0;
}
