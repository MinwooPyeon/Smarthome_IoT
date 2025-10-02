#pragma once

#include "Types.h"
#include <vector>
#include <cmath>

class Analyzer {
public:
    void setAlpha(double at, double ah) { aT = at; aH = ah; }

    // 쾌적도 계산 파라미터 (기본값: 사무실/여름 실내 가정)
    //  - clo: 착의량 (기본 0.5: 반팔+긴바지 정도)
    //  - met: 활동량 (기본 1.1: 앉아서 컴퓨터 작업)
    //  - tr : 평균복사온도(°C). NAN이면 공기온도(tdb)를 사용
    //  - vel: 풍속(m/s)
    void setComfort(double clo, double met, double tr, double vel) {
        m_clo = clo; m_met = met; m_tr = tr; m_vel = vel;
    }

    Metrics compute(const std::vector<EnvSample>& batch);

private:
    // EWMA 상태
    double ewT = NAN, ewH = NAN;
    double aT = 0.2, aH = 0.2;

    // 쾌적도 파라미터
    double m_clo = 0.5;
    double m_met = 1.1;
    double m_tr = NAN;    // 평균복사온도(없으면 tdb 사용)
    double m_vel = 0.1;

    // ----- 계산 유틸 -----
    static double dewPointC(double T, double RH);
    static double heatIndexC(double T, double RH);
    static double absoluteHumidity(double T, double RH);     // g/m^3
    static double wbgtIndoorApprox(double T, double RH);     // °C, 실내 근사식

    // PMV/PPD (Fanger/ISO7730 근사 구현)
    static void pmvPpd(double tdb, double tr, double rh, double vel,
        double met, double clo, double& outPMV, double& outPPD);
};
