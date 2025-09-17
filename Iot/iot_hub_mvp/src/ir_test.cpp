#include "ir_receiver.hpp"
#include <pigpio.h>
#include <iostream>

int main(int argc, char** argv) {
    if (gpioInitialise() < 0) {
        std::cerr << "pigpio init failed\n";
        return 1;
    }

    int pin = 17;   // VS1838B OUT 연결 BCM 핀
    int gapUs = 8000;
    if (argc > 1) pin = std::stoi(argv[1]);

    IrReceiver ir(pin, gapUs);
    if (!ir.init(50)) {
        std::cerr << "ir init failed on pin " << pin << "\n";
        gpioTerminate();
        return 1;
    }

    std::cout << "Press remote button near VS1838B (timeout 3s)...\n";
    auto frame = ir.capture_once(3000);
    if (frame) {
        std::cout << "Captured IR frame (" << frame->raw_us.size() << " pulses)\n";
        for (size_t i = 0; i < frame->raw_us.size(); i++) {
            std::cout << frame->rawUs[i] << (i + 1 < frame->rawUs.size() ? "," : "\n");
        }
    } else {
        std::cout << "IR capture failed (timeout)\n";
    }

    gpioTerminate();
    return 0;
}
