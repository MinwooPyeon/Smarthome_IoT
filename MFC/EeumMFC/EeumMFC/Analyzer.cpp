#include "pch.h"
#include "Analyzer.h"
#include <cmath>

static double dewPointC(double T, double RH) {
    const double a = 17.62, b = 243.12; 
    double g = (a * T) / (b + T) + std::log(RH / 100.0); return (b * g) / (a - g); 
}

static double heatIndexC(double T, double RH) { 
    double Tf = T * 9 / 5 + 32; 
    double HIf = -42.379 + 2.04901523 * Tf + 10.14333127 * RH - 0.22475541 * Tf * RH - 0.00683783 * Tf * Tf - 0.05481717 * RH * RH + 0.00122874 * Tf * Tf * RH + 0.00085282 * Tf * RH * RH - 0.00000199 * Tf * Tf * RH * RH; return (HIf - 32) * 5 / 9; 
}

Metrics Analyzer::compute(const std::vector<EnvSample>& v)
{
    Metrics m{};

    if (v.empty()) return m;
    double sumT = 0, sumH = 0;
    int n = 0;
    for (auto& s : v) {
        if (std::isfinite(s.t))
            sumT += s.t;
        if (std::isfinite(s.h))
            sumH += s.h;
        n++;
    }
    
    m.tAvg = sumT / n;
    m.hAvg = sumH / n;

    const auto& last = v.back();
    ewT = std::isfinite(ewT) ? aT * last.t + (1 - aT) * ewT : last.t;
    ewH = std::isfinite(ewH) ? aH * last.h + (1 - aH) * ewH : last.h;

    m.tEwma = ewT;
    m.hEwma = ewH;
    m.dewPoint = dewPointC(ewT, ewH);
    m.heatIndex = heatIndexC(ewT, ewH);
    m.spike = std::fabs(last.t - m.tAvg) > 5.0;

    return m;
}
