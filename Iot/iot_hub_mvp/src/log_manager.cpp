#include "log_manager.hpp"
#include <iostream>
#include <ctime>

void LogManager::addLog(Log& log){
    std::ofstream outfile(csvPath_, std::ios::app);
    outfile <<  log.tx_id <<','<< log.deviceId <<',' << log.deviceType <<',' << log.function <<',' << log.metaData << '\n';

    outfile.close();
}