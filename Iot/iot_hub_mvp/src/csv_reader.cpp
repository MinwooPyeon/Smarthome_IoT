#include "csv_reader.hpp"
#include "util.hpp"
#include <cstdlib>


Reader::Reader(std::istream& is, Dialect d) : is_(&is), d_(d) {}

std::vector<std::string> Reader::maybe_read_header() {
    if (header_checked_) return {};
    header_checked_ = true;
    std::vector<std::string> row;
    auto pos = is_->tellg();
    if (!read_row(*is_, row, d_)) return {};
    auto is_numeric = [](const std::string& s){
        if (s.empty()) return false; char* end=nullptr; std::strtod(s.c_str(), &end); return *end==0;
    };
    bool likely_header = false;
    for (auto& c : row) { if (!is_numeric(c)) { likely_header = true; break; } }
    if (!likely_header) { is_->clear(); is_->seekg(pos); return {}; }
    return row;
}

bool Reader::next(std::vector<std::string>& fields) { return read_row(*is_, fields, d_); }
