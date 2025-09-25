#pragma once
#include <cstdint>
#include <string>
#include <vector>
#include <optional>
#include <chrono>
#include <istream>
#include <ostream>
#include "types.hpp"

inline int64_t now_ms() {
    using namespace std::chrono;
    return duration_cast<milliseconds>(steady_clock::now().time_since_epoch()).count();
}
// ---------- Low-level CSV ----------
std::string escape_field(const std::string& in, const Dialect& d);
bool read_row(std::istream& is, std::vector<std::string>& out, const Dialect& d);
void write_row(std::ostream& os, const std::vector<std::string>& fields, const Dialect& d);

// ---------- Converters ----------
std::optional<int64_t>  to_i64 (const std::string& s);
std::optional<double>   to_f64 (const std::string& s);
std::optional<bool>     to_bool(const std::string& s);

// ---------- Time (ISO8601, UTC) ----------
std::string to_iso8601(std::chrono::system_clock::time_point tp);
std::optional<std::chrono::system_clock::time_point> from_iso8601(const std::string& s);

// ---------- JSON-ish helpers for arrays ----------
std::string json_encode(const std::vector<int>& v);
std::string json_encode(const std::vector<std::string>& v);
std::vector<int> json_parse_int_array(const std::string& s);
std::vector<std::string> json_parse_str_array(const std::string& s);