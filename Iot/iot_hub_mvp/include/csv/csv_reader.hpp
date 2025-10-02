#pragma once
#include <vector>
#include <string>
#include <istream>
#include "types.hpp"

namespace csv
{
    class Reader
    {
    public:
        Reader(std::istream &is, Dialect d);
        std::vector<std::string> maybe_read_header();
        bool next(std::vector<std::string> &fields);

    private:
        std::istream *is_;
        Dialect d_;
        bool header_checked_ = false;
    };
}