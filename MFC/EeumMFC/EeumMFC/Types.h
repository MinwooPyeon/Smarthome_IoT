#pragma once
#include <string>

struct EnvSample {
	long long tsMs;
	double t, h, gas;
};

struct IrEvent {
	long long tsMs;
	std::string encoding;
};

struct Metrics {
	double tAvg{}, hAvg{};
	double tEwma{}, hEwma{};
	double dewPoint{}, hexIndex{};
	bool spike{};
};