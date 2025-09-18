#include "analyzer.hpp"
#include <cmath>
#include <algorithm>

// static inline bool finite(double x) { return std::isfinite(x); }

// ---- 기본 지표 ----
double Analyzer::dewPointC(double T, double RH) {
    // Magnus-Tetens
    const double a = 17.62, b = 243.12;
    double g = (a * T) / (b + T) + std::log(RH / 100.0);
    return (b * g) / (a - g);
}

double Analyzer::heatIndexC(double T, double RH) {
    // Rothfusz regression (°F) -> °C 변환
    double Tf = T * 9.0 / 5.0 + 32.0;
    double HIf =
        -42.379 + 2.04901523 * Tf + 10.14333127 * RH
        - 0.22475541 * Tf * RH - 0.00683783 * Tf * Tf
        - 0.05481717 * RH * RH + 0.00122874 * Tf * Tf * RH
        + 0.00085282 * Tf * RH * RH - 0.00000199 * Tf * Tf * RH * RH;
    return (HIf - 32.0) * 5.0 / 9.0;
}

// ---- 추가: 절대습도 (g/m^3) ----
// AH = (e * 2.1674) / (273.15 + T), e = RH/100 * 6.112 * exp(17.67T/(T+243.5)) (hPa)
double Analyzer::absoluteHumidity(double T, double RH) {
    double es = 6.112 * std::exp((17.67 * T) / (T + 243.5));       // hPa
    double e = (RH / 100.0) * es;                                 // hPa
    return (e * 2.1674) / (273.15 + T);                            // g/m^3
}

// ---- 추가: WBGT (실내 근사식) ----
// WBGT ≈ 0.567T + 0.393e + 3.94
// e = RH/100 * 6.105 * exp(17.27T/(237.7+T))  (hPa)
double Analyzer::wbgtIndoorApprox(double T, double RH) {
    double e = (RH / 100.0) * 6.105 * std::exp((17.27 * T) / (237.7 + T));
    return 0.567 * T + 0.393 * e + 3.94;
}

// ---- 추가: PMV/PPD ----
// ISO 7730 / Fanger 모델 간략 구현 (입력 단위 주의)
// tdb, tr: °C / rh: % / vel: m/s / met: met / clo: clo
void Analyzer::pmvPpd(double tdb, double tr, double rh, double vel,
    double met, double clo, double& outPMV, double& outPPD) {
    // 수증기압 Pa (rh는 %)
    double pa = rh * 10.0 * std::exp(16.6536 - 4030.183 / (tdb + 235.0));
    double icl = 0.155 * clo;       // m^2*K/W
    double m = met * 58.15;       // W/m^2
    double w = 0.0;               // 외부 일 (없음)
    double mw = m - w;

    // 착의 표면적계수 fcl
    double fcl = (icl > 0.078) ? (1.05 + 0.645 * icl) : (1.0 + 1.29 * icl);

    // 의복 표면온도 수치해 (ISO 공식 기반)
    double tcla = tdb + (35.5 - tdb) / (3.5 * icl + 0.1);
    double p1 = icl * fcl;
    double p2 = p1 * 3.96;
    double p3 = p1 * 100.0;
    double p4 = p1 * tdb;
    double p5 = 308.7 - 0.028 * mw + p2 * std::pow((tr + 273.0) / 100.0, 4);

    double xn = (tcla + 273.0) / 100.0;
    double xf = xn;
    double eps = 0.00015;
    double hc = 0.0;

    for (int i = 0; i < 200; ++i) {
        xf = xn;
        // 대류 열전달 계수 (자연/강제 중 큰 값)
        double hc_nat = 2.38 * std::pow(std::fabs(100.0 * xf - tdb), 0.25);
        double hc_for = 12.1 * std::sqrt((std::max)(0.0, vel));
        hc = (std::max)(hc_nat, hc_for);

        xn = (p5 + p4 * hc - p2 * std::pow(xn, 4)) / (100.0 + p3 * hc);
        if (std::fabs(xn - xf) <= eps) break;
    }

    double tcl = 100.0 * xn - 273.0;

    // 각 열손실 항
    double hl1 = 3.05 * 0.001 * (5733.0 - (6.99 * mw) - pa);
    double hl2 = (mw > 58.15) ? 0.42 * (mw - 58.15) : 0.0;
    double hl3 = 1.7e-5 * m * (5867.0 - pa);
    double hl4 = 0.0014 * m * (34.0 - tdb);
    double hl5 = 3.96 * fcl * (std::pow(xn, 4) - std::pow((tr + 273.0) / 100.0, 4));
    double hl6 = fcl * hc * (tcl - tdb);

    double pmv = (0.303 * std::exp(-0.036 * m) + 0.028) * (mw - hl1 - hl2 - hl3 - hl4 - hl5 - hl6);
    double ppd = 100.0 - 95.0 * std::exp(-0.03353 * std::pow(pmv, 4) - 0.2179 * pmv * pmv);

    outPMV = pmv;
    outPPD = ppd;
}

// ---- 메인 계산 ----
Metrics Analyzer::compute(const std::vector<EnvSample>& v)
{
    Metrics m{};
    if (v.empty()) return m;

    double sumT = 0.0, sumH = 0.0;
    int n = 0;

    for (const auto& s : v) {
        if (finite(s.t)) sumT += s.t;
        if (finite(s.h)) sumH += s.h;
        n++;
    }

    m.tAvg = sumT / (std::max)(1, n);
    m.hAvg = sumH / (std::max)(1, n);

    const auto& last = v.back();

    // EWMA 업데이트
    ewT = finite(ewT) ? (aT * last.t + (1.0 - aT) * ewT) : last.t;
    ewH = finite(ewH) ? (aH * last.h + (1.0 - aH) * ewH) : last.h;

    m.tEwma = ewT;
    m.hEwma = ewH;

    // 기존 지표
    m.dewPoint = dewPointC(ewT, ewH);
    m.heatIndex = heatIndexC(ewT, ewH);
    m.spike = std::fabs(last.t - m.tAvg) > 5.0;

    // 추가 지표
    m.absHumidity = absoluteHumidity(ewT, ewH);
    m.wbgt = wbgtIndoorApprox(ewT, ewH);

    // PMV/PPD (tr이 설정되지 않았다면 공기온도 사용)
    double trUse = std::isfinite(m_tr) ? m_tr : ewT;
    pmvPpd(ewT, trUse, ewH, m_vel, m_met, m_clo, m.pmv, m.ppd);

    return m;
}
