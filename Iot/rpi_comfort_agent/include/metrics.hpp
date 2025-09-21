#pragma once

namespace Metrics {

void pmv_ppd(double ta, double tr, double vel, double rh, double met, double clo,
             double& out_pmv, double& out_ppd);

double wbgt_indoor(double ta, double rh, double tr, double vel);

}
