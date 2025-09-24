#pragma once

#include <string>
#include <vector>
#include <optional>
#include <iosfwd>
#include "types.hpp"

namespace csv{
class CsvInterface{
public:
    std::string escape_field(const std::string& in, const Dialect& d);
    bool read_row(std::istream& is, std::vector<std::string>& out, const Dialect& d);
    void write_row(std::ostream& os, const std::vector<std::string>& fields, const Dialect& d);
protected:
    std::optional<int64_t> to_i64 (const std::string& s);
    std::optional<double> to_f64 (const std::string& s);
    std::optional<bool> to_bool(const std::string& s);
};
}