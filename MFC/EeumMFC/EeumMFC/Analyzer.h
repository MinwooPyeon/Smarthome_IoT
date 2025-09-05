#pragma once

#include "Types.h"
#include <vector>

class Analyzer {
public:
	void setAlpha(double at, double ah) { aT = at; aH = ah; }
	Metrics compute(const std::vector<EnvSample>& batch);
private:
	double ewT = NAN, ewH = NAN;
	double aT = 0.2, aH = 0.2;
};