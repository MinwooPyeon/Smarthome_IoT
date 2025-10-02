#include "HubApp.hpp"
#include <csignal>
using namespace sensors;

HubApp::HubApp(std::string deviceId, std::string mqttHost, int mqttPort)
: deviceId_(std::move(deviceId)),
  pub_(std::make_unique<MqttPublisher>(mqttHost, mqttPort)),
  mqttHelper_(*pub_, deviceId_) {}

HubApp::~HubApp() { stop(); }

bool HubApp::init() {
    if (!pub_->connect("hub-"+deviceId_)) return false;
    mqttHelper_.publishState("online");

    // ---- Sensors
    auto gpio_dht = std::make_shared<GpioPigpio>();
    SensorConfig dhtCfg{.name="dht11_main", .kind="dht11", .pin=27, .sample_interval_ms=2000};
    Dht11Sensor::Options dopt; dopt.pin = dhtCfg.pin;
    dht_ = std::make_shared<Dht11Sensor>(gpio_dht, dhtCfg, dopt);

    auto gpio_ir = std::make_shared<GpioPigpio>();
    SensorConfig irCfg{.name="ir_rx", .kind="nec_ir", .pin=17};
    IrReceiverSensor::Options iopt; iopt.pin = irCfg.pin;
    ir_ = std::make_shared<IrReceiverSensor>(gpio_ir, irCfg, iopt);

    mgr_.registerSensor(dht_);
    mgr_.registerSensor(ir_);

    mgr_.setErrorCallbackForAll([&](const Error& e){
        mqttHelper_.publishError(e.code, e.message);
    });

    dhtWorker_ = std::make_unique<SensorWorker>(dht_, 2000, mqttHelper_);
    irHandler_ = std::make_unique<IrHandler>(ir_, mqttHelper_);

    std::vector<Error> errs;
    mgr_.initializeAll(&errs);
    mgr_.startAll(&errs);
    return true;
}

void HubApp::run() {
    dhtWorker_->start();
    while (running_) {
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }
    stop();
}

void HubApp::stop() {
    if (!running_) return;
    running_ = false;
    if (dhtWorker_) dhtWorker_->stop();
    mgr_.stopAll();
    mqttHelper_.publishState("offline");
    pub_->disconnect();
}
