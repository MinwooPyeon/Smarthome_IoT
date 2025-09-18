#include "metrics.hpp"
#include <cmath>
#include <algorithm>

static inline double clampd_local(double v, double lo, double hi){
    return std::max(lo, std::min(v, hi));
}

void Metrics::pmv_ppd(double ta, double tr, double vel, double rh, double met, double clo,
                      double& out_pmv, double& out_ppd){
    double pa  = rh/100.0 * 10.0 * std::exp(16.6536 - 4030.183/(ta+235.0));
    double M   = met*58.15;
    double Icl = 0.155*clo;
    double fcl = (Icl>0.078) ? (1.05 + 0.645*Icl) : (1.0 + 0.2*Icl);
    double hcf = 12.1 * std::sqrt(std::max(vel,1e-6));
    double taa = ta + 273.0;
    double tra = tr + 273.0;
    double tcla = taa + (35.5 - ta)/(3.5*(6.45*Icl + 0.1));
    for(int i=0;i<20;i++){
        double hcn = 2.38*std::pow(std::fabs(100.0*(tcla - taa)), 0.25);
        double hc  = std::max(hcf, hcn);
        (void)hc; // 간략 수렴(상수로 흡수), 구현 단순화
        double tcl = taa + (tra - tcla)*0.3;
        tcla = 0.5*(tcla + tcl);
    }
    double tcl = tcla;
    double hc  = std::max(hcf, 2.38*std::pow(std::fabs(100.0*(tcl - taa)), 0.25));

    double hl1 = 3.05*(5.733 - 0.007*(M) - pa);
    double hl2 = 0.42*(M - 58.15);
    double hl3 = 0.0173*M*(5.867 - pa);
    double hl4 = 0.0014*M*(34.0 - ta);
    double hl5 = 3.96e-8*fcl*(std::pow(tra,4) - std::pow(tcl,4));
    double hl6 = fcl*hc*(tcl - taa);

    double pmv = (0.303*std::exp(-0.036*M)+0.028) * (M - hl1 - hl2 - hl3 - hl4 - hl5 - hl6);
    double ppd = 100.0 - 95.0*std::exp(-0.03353*pmv*pmv*pmv*pmv - 0.2179*pmv*pmv);
    out_pmv = clampd_local(pmv, -3.0, 3.0);
    out_ppd = clampd_local(ppd, 0.0, 100.0);
}

double Metrics::wbgt_indoor(double ta, double rh, double tr, double vel){
    double t_wb_like = ta - (100.0 - rh)*0.05*0.1 + 0.2*vel;
    return 0.7*t_wb_like + 0.3*tr;
}
