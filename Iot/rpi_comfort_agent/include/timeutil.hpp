#pragma once
#include <utility>

class TimeUtil {
public:
    // returns {start, end} for yesterday 00:00 ~ today 00:00 (local time)
    static std::pair<long,long> YesterdayRangeEpoch();
};
