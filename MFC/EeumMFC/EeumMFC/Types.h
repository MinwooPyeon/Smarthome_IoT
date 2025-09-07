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
    // 기존 값
    double tAvg = NAN;     // 평균 온도
    double hAvg = NAN;     // 평균 습도
    double tEwma = NAN;    // EWMA 온도
    double hEwma = NAN;    // EWMA 습도
    double dewPoint = NAN; // 이슬점(°C)
    double heatIndex = NAN;// 체감온도(°C)
    bool   spike = false;

    // 추가: 공조/쾌적 지표
    double absHumidity = NAN; // 절대습도 (g/m^3)
    double wbgt = NAN;        // WBGT (근사, °C)
    double pmv = NAN;         // PMV (-3 ~ +3)
    double ppd = NAN;         // PPD (%)
};