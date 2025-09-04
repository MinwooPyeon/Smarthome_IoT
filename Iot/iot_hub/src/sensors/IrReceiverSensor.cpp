#include "sensors/IrReceiverSensor.hpp"

using namespace sensors;

IrReceiverSensor::IrReceiverSensor(std::shared_ptr<iGpio> gpio, SensorConfig cfg, Options opt)
    : gpio_(std::move(gpio)), cfg_(std::move(cfg)), opt_(opt){}

bool IrReceiverSensor::initialize(Error* err){
    if(!gpio_){
        if(err)
            err->message = "gpio null";
        state_ = SensorState::Fault;
        return false;
    }
    int gerr{};

    if(!gpio_->open(opt_.chip, opt_.pin, PinMode::INPUT, &gerr)){
        if(err)
            err->message = "gpio open failed, err="+std::to_string(gerr);
        state_ = SensorState::Fault;
        return false;
    }

    state_ = SensorState::Ready;
    return true;
}

bool IrReceiverSensor::start(Error* err){
    if(state_ != SensorState::Ready && state_ != SensorState::Stopped) return true;

    int gerr{};

    if(!gpio_->watch(Edge::BOTH, [this](int pin, bool level, uint32_t tick){
        this->on_edge(level, tick);
    }, &gerr)){
        if(err)
            err->message = "watch failed, err=" + std::to_string(gerr);
        state_ = SensorState::Fault;
        return false;
    }

    state_ = SensorState::Running;
    return true;
}

void IrReceiverSensor::stop(){
    if(state_ == SensorState::Running || state_ == SensorState::Ready){
        gpio_->unwatch();
        gpio_->close();
        state_ = SensorState::Stopped;
    }
}

bool IrReceiverSensor::reset(Error* err){
    stop();
    return initialize(err) && start(err);
}

void IrReceiverSensor::clear_decoder(){
    std::lock_guard lk(m_);
    seq_.clear();
    last_tick_ = 0;
    last_level_ = true;
}

void IrReceiverSensor::on_edge(bool level, uint32_t tick){
    std::lock_guard lk(m_);
    if(last_tick_ == 0){
        last_tick_ = tick;
        return;
    }

    uint32_t dt = tick - last_tick_;
    last_tick_ = tick;

    seq_.push_back(static_cast<int>(dt));

    if(dt > 20000){
        try_decode_frame();
        seq_.clear();
    }
}

bool IrReceiverSensor::try_decode_frame(){
    if(seq_.size() < 2+ 2*32) return false;

    const int tol = opt_.tol;
    for(size_t i =0;i<seq_.size();i++){
        int low = seq_[i];
        int high = seq_[i+1];
        if(near(low, 9000, 900) && near(high, 4500, 500)){
            size_t idx = i+2;
            uint32_t data = 0;
            for(int b = 0;b<32;b++){
                if(idx+1 >= seq_.size()) return false;
                int low_b = seq_[idx];
                int high_b = seq_[idx+1];

                if(!near(low_b, 560, 200)) return false;

                int bit = near(high_b, 1690, 400) ? 1:(near(high_b,500, 200)? 0 : -1);
                if(bit < 0) return false;

                data |=(bit ? (1u << b) : 0u);
                idx += 2;
            }

            uint8_t addr = data & 0xFF;
            uint8_t naddr = (data >> 8) & 0xFF;
            uint8_t cmd = (data >> 16) & 0xFF;
            uint8_t ncmd = (data >> 24) & 0xFF;
            if((uint8_t)~addr != naddr || (uint8_t)~cmd != ncmd) return false;

            SensorReading r{};
            r.name = cfg_.name;
            r.kind = cfg_.kind;
            r.ts_ms = now_ms();
            r.values["nec_address"] = addr;
            r.values["nec_command"] = cmd;
            r.values["raw32"] = static_cast<double>(data);

            last_frame_ =r;
            if(on_read_) on_read_(r);
            return true;
        }
    }
    return false;
}

std::optional<SensorReading> IrReceiverSensor::readOnce(Error* err){
    return last_frame_;
}