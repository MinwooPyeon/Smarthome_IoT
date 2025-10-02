#pragma once

#include "types.hpp"
#include <vector>
#include <cmath>

class Analyzer {
public:
    void setAlpha(double at, double ah) { aT = at; aH = ah; }

    // ������ ��� �Ķ���� (�⺻��: �繫��/���� �ǳ� ����)
    //  - clo: ���Ƿ� (�⺻ 0.5: ����+����� ����)
    //  - met: Ȱ���� (�⺻ 1.1: �ɾƼ� ��ǻ�� �۾�)
    //  - tr : ��պ���µ�(��C). NAN�̸� ����µ�(tdb)�� ���
    //  - vel: ǳ��(m/s)
    void setComfort(double clo, double met, double tr, double vel) {
        m_clo = clo; m_met = met; m_tr = tr; m_vel = vel;
    }

    Metrics compute(const std::vector<EnvSample>& batch);

private:
    // EWMA ����
    double ewT = NAN, ewH = NAN;
    double aT = 0.2, aH = 0.2;

    // ������ �Ķ����
    double m_clo = 0.5;
    double m_met = 1.1;
    double m_tr = NAN;    // ��պ���µ�(������ tdb ���)
    double m_vel = 0.1;

    // ----- ��� ��ƿ -----
    static double dewPointC(double T, double RH);
    static double heatIndexC(double T, double RH);
    static double absoluteHumidity(double T, double RH);     // g/m^3
    static double wbgtIndoorApprox(double T, double RH);     // ��C, �ǳ� �ٻ��

    // PMV/PPD (Fanger/ISO7730 �ٻ� ����)
    static void pmvPpd(double tdb, double tr, double rh, double vel,
        double met, double clo, double& outPMV, double& outPPD);
};
