#pragma once
#include "types.hpp"
#include "params.hpp"
#include <vector>
#include <string>

struct OptimizeConfig {
    // 보수적 그리드
    std::vector<double> alphas {0.15, 0.20, 0.25, 0.30};
    std::vector<double> clos   {0.50, 0.60, 0.80};
    std::vector<double> mets   {1.00, 1.10, 1.20};
    std::vector<double> troffs {-0.3, 0.0, 0.3};
    std::vector<double> vels   {0.08, 0.10, 0.15};

    int windowSec{1200};      // 20분
    int reopSec{600};         // 10분(재조작 판정)
    double penalty{1.5};      // 불만 가중
};

class Optimizer {
public:
    static Params Run(const std::vector<EnvRow>& env,
                      const std::vector<LogRow>& log,
                      const OptimizeConfig& cfg);
private:
    static std::vector<Session> LabelSessions(const std::vector<LogRow>& log, int reopSec);
    static bool scoreThetaForSession(const std::vector<EnvRow>& env, long event_ts,
                                     const Params& theta, int windowSec, double& Jout);
};
