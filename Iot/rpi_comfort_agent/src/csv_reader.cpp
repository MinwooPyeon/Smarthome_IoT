#include "csv_reader.hpp"
#include <fstream>
#include <sstream>

std::vector<std::string> CsvReader::splitCsvLine(const std::string& line){
    std::vector<std::string> out;
    std::string cur; bool in_q=false;
    for(char c: line){
        if(c=='"'){ in_q=!in_q; continue; }
        if(c==',' && !in_q){ out.push_back(cur); cur.clear(); }
        else cur.push_back(c);
    }
    out.push_back(cur);
    return out;
}

std::unordered_map<std::string,int> CsvReader::headerIndex(const std::string& header){
    auto cols = splitCsvLine(header);
    std::unordered_map<std::string,int> m;
    for(int i=0;i<(int)cols.size();++i) m[cols[i]] = i;
    return m;
}

template<typename T>
bool CsvReader::parseNum(const std::string& s, T& v){
    try{
        if constexpr (std::is_integral<T>::value) v = (T)std::stoll(s);
        else v = (T)std::stod(s);
        return true;
    }catch(...){ return false; }
}

bool CsvReader::LoadEnvCsv(const std::string& path, long start, long end, std::vector<EnvRow>& out){
    std::ifstream f(path);
    if(!f) return false;
    std::string line;
    if(!std::getline(f,line)) return false;
    auto idx = headerIndex(line);

    auto get = [&](const std::vector<std::string>& cols, const char* k)->std::string{
        auto it=idx.find(k); if(it==idx.end()) return "";
        int i=it->second; return i<(int)cols.size()? cols[i] : "";
    };

    while(std::getline(f,line)){
        if(line.empty()) continue;
        auto cols = splitCsvLine(line);
        EnvRow r{};
        if(!parseNum(get(cols,"ts"), r.ts)) continue;
        if(r.ts < start || r.ts >= end) continue;
        parseNum(get(cols,"tAvg"),  r.tAvg);
        parseNum(get(cols,"hAvg"),  r.hAvg);
        parseNum(get(cols,"tEwma"), r.tEwma);
        parseNum(get(cols,"hEwma"), r.hEwma);
        out.push_back(r);
    }
    return true;
}

bool CsvReader::LoadLogCsv(const std::string& path, long start, long end, std::vector<LogRow>& out){
    std::ifstream f(path);
    if(!f) return false;
    std::string line;
    if(!std::getline(f,line)) return false;
    auto idx = headerIndex(line);

    auto get = [&](const std::vector<std::string>& cols, const char* k)->std::string{
        auto it=idx.find(k); if(it==idx.end()) return "";
        int i=it->second; return i<(int)cols.size()? cols[i] : "";
    };

    while(std::getline(f,line)){
        if(line.empty()) continue;
        auto cols = splitCsvLine(line);
        LogRow r{};
        if(!parseNum(get(cols,"ts"), r.ts)) continue;
        if(r.ts < start || r.ts >= end) continue;
        r.deviceId    = get(cols,"deviceId");
        r.device_type = get(cols,"device_type");
        r.function    = get(cols,"function");
        r.meta_data   = get(cols,"meta_data");
        out.push_back(r);
    }
    return true;
}
