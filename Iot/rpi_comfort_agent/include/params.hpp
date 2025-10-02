#pragma once
#include <algorithm>
#include <nlohmann/json.hpp>

inline double clampd(double v, double lo, double hi) {
    return std::max(lo, std::min(v, hi));
}

struct Params {
    double alphaT{0.2};
    double alphaH{0.2};
    double clo{0.5};
    double met{1.1};
    double trOffset{0.0};
    double vel{0.1};

    void clip() {
        alphaT = clampd(alphaT, 0.05, 0.6);
        alphaH = clampd(alphaH, 0.05, 0.6);
        clo    = clampd(clo,    0.30, 1.20);
        met    = clampd(met,    0.80, 2.00);
        trOffset = clampd(trOffset, -2.0, 2.0);
        vel    = clampd(vel,    0.05, 0.60);
    }

    nlohmann::json to_json() const {
        return nlohmann::json{
            {"alphaT",alphaT},{"alphaH",alphaH},
            {"clo",clo},{"met",met},
            {"trOffset",trOffset},{"vel",vel}
        };
    }
};
