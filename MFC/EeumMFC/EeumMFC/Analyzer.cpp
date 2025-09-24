#include "pch.h"
#include "Analyzer.h"
#include <cmath>
#include <algorithm>

static inline bool finite(double x) { return std::isfinite(x); }
template<typename T>
static inline T clamp(T x, T lo, T hi) {
    return (x < lo) ? lo : (x > hi) ? hi : x;
}
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
    double met, double clo, double& outPMV, double& outPPD)
{
    // --- 수증기압 [Pa] : Tetens + RH% ---
    const double p_ws = 610.5 * std::exp(17.2694 * tdb / (237.29 + tdb));
    const double pa = (rh / 100.0) * p_ws;

    const double icl = 0.155 * clo;      // m^2*K/W
    const double m = met * 58.15;      // W/m^2
    const double w = 0.0;
    const double mw = m - w;

    const double fcl = (icl > 0.078) ? (1.05 + 0.645 * icl) : (1.0 + 1.29 * icl);

    const double tcla = tdb + (35.5 - tdb) / (3.5 * icl + 0.1);
    const double p1 = icl * fcl, p2 = p1 * 3.96, p3 = p1 * 100.0, p4 = p3 * ((tdb + 273.0) / 100.0);
    const double p5 = 308.7 - 0.028 * mw + p2 * std::pow((tr + 273.0) / 100.0, 4);

    double xn = (tcla + 273.0) / 100.0;
    double xf = xn;
    const double eps = 1.5e-4;
    double hc = 0.0;


    for (int i = 0; i < 200; ++i) {

        xf = xn;
        const double tcl_prev = 100.0 * xf - 273.0; // °C
        const double hc_nat = 2.38 * std::pow(std::fabs(tcl_prev - tdb), 0.25);
        const double hc_for = 12.1 * std::sqrt((std::max)(0.0, vel));
        hc = (std::max)(hc_nat, hc_for);
        xn = (p5 + p4 * hc - p2 * std::pow(xf, 4)) / (100.0 + p3 * hc);
        
        if (std::fabs(xn - xf) <= eps) break;
    }

    const double tcl = 100.0 * xn - 273.0;

    const double hl1 = 3.05e-3 * (5733.0 - 6.99 * mw - pa);
    const double hl2 = (mw > 58.15) ? 0.42 * (mw - 58.15) : 0.0;
    const double hl3 = 1.7e-5 * m * (5867.0 - pa);
    const double hl4 = 1.4e-3 * m * (34.0 - tdb);
    const double hl5 = 3.96 * fcl * (std::pow((tcl + 273.0) / 100.0, 4) - std::pow((tr + 273.0) / 100.0, 4));
    const double hl6 = fcl * hc * (tcl - tdb);

    const double pmv = (0.303 * std::exp(-0.036 * m) + 0.028) * (mw - hl1 - hl2 - hl3 - hl4 - hl5 - hl6);
    const double ppd = 100.0 - 95.0 * std::exp(-0.03353 * std::pow(pmv, 4) - 0.2179 * pmv * pmv);

    outPMV = pmv;
    outPPD = clamp(ppd, 0.0, 100.0);

}


// ---- 메인 계산 ----
Metrics Analyzer::compute(const std::vector<EnvSample>& v)
{
    using std::isfinite;
    using std::numeric_limits;

    Metrics m{};
    if (v.empty()) return m; // 그대로 반환(필요하면 NaN으로 초기화해도 됨)

    double sumT = 0.0, sumH = 0.0;
    int nT = 0, nH = 0;

    for (const auto& s : v) {
        if (isfinite(s.t)) { sumT += s.t; ++nT; }
        if (isfinite(s.h)) { sumH += s.h; ++nH; }
    }

    m.tAvg = (nT > 0) ? (sumT / nT) : numeric_limits<double>::quiet_NaN();
    m.hAvg = (nH > 0) ? (sumH / nH) : numeric_limits<double>::quiet_NaN();

    const auto& last = v.back();
    const double lastT = isfinite(last.t) ? last.t : (isfinite(m.tAvg) ? m.tAvg : ewT);
    const double lastH = isfinite(last.h) ? last.h : (isfinite(m.hAvg) ? m.hAvg : ewH);

    // alpha 안전범위
    const double alphaT = clamp(aT, 0.0, 1.0);
    const double alphaH = clamp(aH, 0.0, 1.0);

    // EWMA 업데이트: 최신값이 유효할 때만 갱신
    if (isfinite(lastT)) ewT = isfinite(ewT) ? (alphaT * lastT + (1.0 - alphaT) * ewT) : lastT;
    if (isfinite(lastH)) ewH = isfinite(ewH) ? (alphaH * lastH + (1.0 - alphaH) * ewH) : lastH;

    m.tEwma = ewT;
    m.hEwma = ewH;

    // RH 범위 보정(필요시)
    const double rh = isfinite(ewH) ? clamp(ewH, 0.0, 100.0) : ewH;

    // 파생지표는 입력값 유효할 때만 계산
    if (isfinite(ewT) && isfinite(rh)) {
        m.dewPoint = dewPointC(ewT, rh);
        m.heatIndex = heatIndexC(ewT, rh);       // 구현이 °C+% 입력인지 확인
        m.absHumidity = absoluteHumidity(ewT, rh);
        m.wbgt = wbgtIndoorApprox(ewT, rh);
    }
    else {
        m.dewPoint = m.heatIndex = m.absHumidity = m.wbgt =
            numeric_limits<double>::quiet_NaN();
    }

    // 스파이크: 평균보다 EWMA 기준이 일반적으로 더 안정적
    const double baseline = isfinite(m.tEwma) ? m.tEwma : m.tAvg;
    m.spike = (isfinite(last.t) && isfinite(baseline)) ? (std::fabs(last.t - baseline) > 5.0) : false;

    // PMV/PPD
    const double trUse = std::isfinite(m_tr) ? m_tr : ewT;
    if (isfinite(ewT) && isfinite(trUse) && isfinite(rh)) {
        
        // NOTE: pmvPpd의 rh 인자 단위를 확인하세요(%, 혹은 0~1).
        pmvPpd(ewT, trUse, rh, m_vel, m_met, m_clo, m.pmv, m.ppd);

    }
    else {
        m.pmv = m.ppd = numeric_limits<double>::quiet_NaN();
    }

    return m;
}

