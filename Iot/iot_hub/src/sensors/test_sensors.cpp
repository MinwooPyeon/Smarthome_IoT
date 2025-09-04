#include "sensors/sensorManager.hpp"
#include "interface/gpioPigpio.hpp"
#include "sensors/Dht11Sensor.hpp"
#include "sensors/IrReceiverSensor.hpp"
#include <iostream>

using namespace sensors;

int main() {
    sensorManager mgr;

    // DHT11
    auto gpio_dht = std::make_shared<GpioPigpio>();
    SensorConfig dhtCfg;
    dhtCfg.name = "dht11_living";
    dhtCfg.kind = "dht11";
    dhtCfg.pin  = 4;
    Dht11Sensor::Options dopt;
    dopt.pin = dhtCfg.pin;
    auto dht = std::make_shared<Dht11Sensor>(gpio_dht, dhtCfg, dopt);

    // IR Receiver (VS1838/VS1836 등)
    auto gpio_ir = std::make_shared<GpioPigpio>();
    SensorConfig irCfg;
    irCfg.name = "ir_rx";
    irCfg.kind = "nec_ir";
    irCfg.pin  = 17;
    IrReceiverSensor::Options iopt;
    iopt.pin = irCfg.pin;
    auto ir = std::make_shared<IrReceiverSensor>(gpio_ir, irCfg, iopt);

    mgr.registerSensor(dht);
    mgr.registerSensor(ir);

    mgr.setReadingCallbackForAll([](const SensorReading& r){
        std::cout << "[" << r.ts_ms << "] " << r.name << "(" << r.kind << ")";
        for (auto& kv : r.values) {
            std::cout << " " << kv.first << "=" << kv.second;
        }
        if (r.text) std::cout << " text=" << *r.text;
        std::cout << "\n";
    });
    mgr.setErrorCallbackForAll([](const Error& e){
        std::cerr << "Sensor error: (" << e.code << ") " << e.message << "\n";
    });

    std::vector<Error> errs;
    mgr.initializeAll(&errs);
    mgr.startAll(&errs);

    // DHT11 1회 측정 (이벤트/폴링 혼합)
    Error e{};
    if (auto rd = dht->readOnce(&e)) {
        // 사용
    } else if (!e.message.empty()) {
        std::cerr << "DHT11 readOnce failed: " << e.message << "\n";
    }

    // IR 수신은 리모컨 누르면 콜백으로 수신됨

    // ...
    mgr.stopAll();
    return 0;
}
