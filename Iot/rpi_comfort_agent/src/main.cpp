#include "csv_reader.hpp"
#include "timeutil.hpp"
#include "optimizer.hpp"

#include "params.hpp"
#include <nlohmann/json.hpp>
#include <fstream>
#include <iostream>

using nlohmann::json;

// === 경로/설정 (필요 시 수정) ===
static const std::string ENV_CSV     = "env.csv";
static const std::string LOG_CSV   = "log.csv";
static const std::string PARAMS_JSON = "params.json";
static const std::string DEVICE_ID_FALLBACK = "rasp-1";
static const std::string MQTT_HOST   = "localhost";
static const int         MQTT_PORT   = 1883;

static Params loadPrev(){
    std::ifstream f(PARAMS_JSON);
    if(!f) return Params{};
    json j; f>>j;
    Params p;
    if(j.contains("alphaT")) p.alphaT = j["alphaT"].get<double>();
    if(j.contains("alphaH")) p.alphaH = j["alphaH"].get<double>();
    if(j.contains("clo"))    p.clo    = j["clo"].get<double>();
    if(j.contains("met"))    p.met    = j["met"].get<double>();
    if(j.contains("trOffset")) p.trOffset = j["trOffset"].get<double>();
    if(j.contains("vel"))    p.vel    = j["vel"].get<double>();
    p.clip();
    return p;
}

static void save(const Params& p){
    std::ofstream f(PARAMS_JSON);
    f << p.to_json().dump(2);
}

int main(){
    auto [start, end] = TimeUtil::YesterdayRangeEpoch();

    std::vector<EnvRow> envs;
    std::vector<LogRow>  logs;

    if(!CsvReader::LoadEnvCsv(ENV_CSV, start, end, envs)){
        std::cerr << "[WARN] log.csv load failed or empty\n";
    }
    if(!CsvReader::LoadLogCsv(LOG_CSV, start, end, logs)){
        std::cerr << "[WARN] IrDevice.csv load failed or empty\n";
    }
    if(envs.empty() || logs.empty()){
        std::cerr << "[WARN] insufficient data → reuse previous params\n";
        auto prev = loadPrev(); prev.clip();
        save(prev);
        return 0;
    }

    OptimizeConfig cfg; // 기본값 사용(20분 창, 10분 재조작 판정, 보수적 그리드)
    Params best = Optimizer::Run(envs, logs, cfg);
    best.clip();
    save(best);
    return 0;
}
