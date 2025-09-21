#pragma once
#include "types.hpp"
#include <string>
#include <unordered_map>
#include <vector>

class CsvReader {
public:
    static bool LoadEnvCsv(const std::string& path, long start, long end, std::vector<EnvRow>& out);
    static bool LoadLogCsv (const std::string& path, long start, long end, std::vector<LogRow>& out);

private:
    static std::vector<std::string> splitCsvLine(const std::string& line);
    static std::unordered_map<std::string,int> headerIndex(const std::string& header);
    template<typename T> static bool parseNum(const std::string& s, T& v);
};
