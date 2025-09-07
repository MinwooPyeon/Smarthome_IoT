#pragma once
#include "MsgBuffer.h"
#include "MqttClient.h"
#include "Timer.h"
#include "Analyzer.h"

class Ingestor {
public:
	using MetricsCallback = std::function<void(const std::vector<EnvSample>&, const std::vector<IrEvent>&, const Metrics&)>;
private:
	MsgBuffer<EnvSample> envBuf_;
	MsgBuffer<IrEvent> irBuf_;
	Analyzer analyzer_;
	Timer timer_;
	MetricsCallback onTick_;
public:
	void setCallback(MetricsCallback callback);
	void start(double hz);
	void stop();
	
	void pushEnv(const EnvSample& env);
	void pushIr(const IrEvent& ir);
private:
	void tickOnce();
};