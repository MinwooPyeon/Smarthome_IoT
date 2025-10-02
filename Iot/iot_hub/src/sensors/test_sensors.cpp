#include "sensors/sensorManager.hpp"
#include "interface/gpioPigpio.hpp"
#include "sensors/Dht11Sensor.hpp"
#include "sensors/IrReceiverSensor.hpp"
#include <iostream>
#include <thread>
#include <chrono>

using namespace sensors;

int main() {
    sensorManager mgr;

    // ✅ pigpio 래퍼는 하나만
    auto gpio = std::make_shared<GpioPigpio>(); // libpigpio 기반이라고 가정

    // ✅ DHT11 설정 (BCM 4 권장)
    SensorConfig dhtCfg;
    dhtCfg.name = "dht11_living";
    dhtCfg.kind = "dht11";
    dhtCfg.pin  = 27; 
    Dht11Sensor::Options dopt;
    dopt.pin = dhtCfg.pin;
    auto dht = std::make_shared<Dht11Sensor>(gpio, dhtCfg, dopt);

    // ✅ IR 수신기 설정 (BCM 18 권장)
    SensorConfig irCfg;
    irCfg.name = "ir_rx";
    irCfg.kind = "nec_ir";
    irCfg.pin  = 17; 
    IrReceiverSensor::Options iopt;
    iopt.pin = irCfg.pin;
    auto ir = std::make_shared<IrReceiverSensor>(gpio, irCfg, iopt);

    mgr.registerSensor(dht);
    mgr.registerSensor(ir);

    mgr.setReadingCallbackForAll([](const SensorReading& r){
        std::cout << "[" << r.ts_ms << "] " << r.name << "(" << r.kind << ")";
        for (auto& kv : r.values) std::cout << " " << kv.first << "=" << kv.second;
        if (r.text) std::cout << " text=" << *r.text;
        std::cout << "\n";
    });
    mgr.setErrorCallbackForAll([](const Error& e){
        std::cerr << "Sensor error: (" << e.code << ") " << e.message << "\n";
    });

    std::vector<Error> errs;
    mgr.initializeAll(&errs);
    for (auto& e: errs) std::cerr << "[init] ("<<e.code<<") "<<e.message<<"\n";
    errs.clear();

    mgr.startAll(&errs);
    for (auto& e: errs) std::cerr << "[start] ("<<e.code<<") "<<e.message<<"\n";

    // ✅ DHT11 1회 읽기 (start 이후)
    // test_sensors.cpp (발췌)
    Error e{};
    for (int i = 0; i < 5; i++) {
      if (auto rd = dht->readOnce(&e)) {
        // ✅ 온도와 습도 출력
        double temp = rd->values.count("temperature_c") ? rd->values.at("temperature_c") : -999;
        double hum  = rd->values.count("humidity_rh")   ? rd->values.at("humidity_rh")   : -999;
        
        std::cout << "[DHT11] "
                  << "Temp=" << temp << " °C, "
                  << "Hum="  << hum  << " %"
                  << " (ts=" << rd->ts_ms << ")\n";
    } else if (!e.message.empty()) {
        std::cerr << "DHT11 readOnce failed: " << e.message << "\n";
    }

    std::this_thread::sleep_for(std::chrono::seconds(2)); // 2초 간격
}


    // ... 리모컨 누르면 ir 콜백 발생

    mgr.stopAll();
    return 0;
}
