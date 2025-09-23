#include "pch.h"
#include "Ingestor.h"

void Ingestor::setCallback(MetricsCallback callback) {
	onTick_ = std::move(callback);
}

void Ingestor::start(double hz) {
	timer_.start(hz, [this] {this->tickOnce(); });
}

void Ingestor::stop() {
	timer_.stop();
}

void Ingestor::pushEnv(const EnvSample& env) {
	envBuf_.push(env);
}
	
void Ingestor::pushIr(const IrEvent& ir) {
	irBuf_.push(ir);
}

void Ingestor::tickOnce() {
	OutputDebugStringW(L"[Ingestor] tickOnce()\r\n");
	auto env = envBuf_.flush();
	auto ir = irBuf_.flush();

	if (env.empty() && ir.empty()) {
		OutputDebugStringW(L"[Ingestor] buffers empty -> skip\r\n");
		return;
	}
	
	auto met = analyzer_.compute(env);
	if (onTick_) onTick_(env, ir, met);
}