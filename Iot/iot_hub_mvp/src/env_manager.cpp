#include "env_manager.hpp"
#include <fstream>

void EnvManager::addData(EnvSample& env){
    std::ofstream outfile(csvPath_, std::ios::app);
    outfile <<  env.tsMs <<','<< env.t <<',' << env.h <<'\n';

    outfile.close();
}