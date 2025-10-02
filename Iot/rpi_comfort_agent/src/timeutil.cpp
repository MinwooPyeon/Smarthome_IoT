#include "timeutil.hpp"
#include <chrono>
#include <ctime>

std::pair<long,long> TimeUtil::YesterdayRangeEpoch(){
    using namespace std::chrono;
    auto now = system_clock::now();
    std::time_t tnow = system_clock::to_time_t(now);
    std::tm lt = *std::localtime(&tnow);
    lt.tm_hour=0; lt.tm_min=0; lt.tm_sec=0;
    std::time_t today0 = std::mktime(&lt);
    long end   = (long)today0;
    long start = end - 86400;
    return {start, end};
}
