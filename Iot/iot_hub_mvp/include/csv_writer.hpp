#pragma once
#include <vector>
#include <string>
#include <ostream>
#include "types.hpp"


class Writer {
public:
    Writer(std::ostream& os, Dialect d, std::vector<std::string> header = {});
    void header(const std::vector<std::string>& names); // override header
    void write(const std::vector<std::string>& fields);
private:
    std::ostream* os_;
    Dialect d_;
    bool header_written_ = false;
    std::vector<std::string> header_;
};
