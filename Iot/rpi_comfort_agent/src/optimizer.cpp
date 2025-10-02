#include "optimizer.hpp"
#include "metrics.hpp"
#include <algorithm>
#include <cmath>

std::vector<Session> Optimizer::LabelSessions(const std::vector<LogRow>& log, int reopSec){
    std::vector<Session> out;
    if(log.empty()) return out;
    auto sorted = log;
    std::sort(sorted.begin(), sorted.end(),
              [](const LogRow& a, const LogRow& b){ return a.ts < b.ts; });
    for(size_t i=0;i<sorted.size();++i){
        const auto& r = sorted[i];
        int outcome = 1;
        for(size_t j=i+1;j<sorted.size();++j){
            const auto& nx = sorted[j];
            if(nx.ts > r.ts + reopSec) break;
            if(nx.device_type==r.device_type && nx.function==r.function){
                outcome = 0; break;
            }
        }
        out.push_back(Session{r.ts, r.device_type, r.function, r.meta_data, outcome});
    }
    return out;
}

bool Optimizer::scoreThetaForSession(const std::vector<EnvRow>& env, long event_ts,
                                     const Params& theta, int windowSec, double& Jout){
    if(env.empty()) return false;
    // 이벤트 이후 windowSec 동안의 평균 tEwma/hEwma
    double sumT=0, sumH=0; int n=0;
    for(const auto& r: env){
        if(r.ts>=event_ts && r.ts<event_ts+windowSec){
            double ta = (r.tEwma!=0.0 ? r.tEwma : r.tAvg);
            double rh = (r.hEwma!=0.0 ? r.hEwma : r.hAvg);
            sumT += ta; sumH += rh; ++n;
        }
    }
    if(n==0) return false;
    double ta = sumT/n;
    double rh = sumH/n;
    double tr = ta + theta.trOffset;
    double pmv, ppd;
    Metrics::pmv_ppd(ta, tr, theta.vel, rh, theta.met, theta.clo, pmv, ppd);
    double w = Metrics::wbgt_indoor(ta, rh, tr, theta.vel);
    double J = std::fabs(pmv) + 0.05*ppd + 0.2*std::max(0.0, w-28.0);
    Jout = J;
    return true;
}

Params Optimizer::Run(const std::vector<EnvRow>& env,
                      const std::vector<LogRow>& log,
                      const OptimizeConfig& cfg){
    // 세션 라벨
    auto sessions = LabelSessions(log, cfg.reopSec);
    if(sessions.empty()){
        Params prev; prev.clip(); return prev;
    }

    // 그리드 탐색
    Params best;
    double bestScore = 1e18;
    bool found=false;

    for(double aT: cfg.alphas)
    for(double aH: cfg.alphas)
    for(double cl: cfg.clos)
    for(double me: cfg.mets)
    for(double to: cfg.troffs)
    for(double ve: cfg.vels){
        Params th{aT,aH,cl,me,to,ve};
        th.clip();
        double total=0.0; int valid=0;
        for(const auto& s: sessions){
            double J;
            if(!scoreThetaForSession(env, s.event_ts, th, cfg.windowSec, J)) continue;
            total += (s.outcome==1 ? J : cfg.penalty*J);
            ++valid;
        }
        if(valid >= std::max(1, (int)std::ceil(0.3*sessions.size()))){
            if(total < bestScore){ bestScore=total; best=th; found=true; }
        }
    }

    if(!found){ Params prev; prev.clip(); return prev; }
    return best;
}
