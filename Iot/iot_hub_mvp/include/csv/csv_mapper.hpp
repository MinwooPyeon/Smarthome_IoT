#pragma once
#include <string>
#include <vector>
#include <functional>
#include <utility>

namespace csv {

template <typename T>
struct Column {
    std::string name;
    std::function<std::string(const T&)> getter;            // T -> string
    std::function<void(T&, const std::string&)> setter;      // string -> T

    // 안전한 명시적 생성자(aggregate 초기화 대신)
    Column(std::string n,
           std::function<std::string(const T&)> g,
           std::function<void(T&, const std::string&)> s)
        : name(std::move(n)), getter(std::move(g)), setter(std::move(s)) {}
};

// 가독성을 위한 헬퍼
template <typename T>
inline Column<T> make_column(std::string name,
                             std::function<std::string(const T&)> getter,
                             std::function<void(T&, const std::string&)> setter) {
    return Column<T>(std::move(name), std::move(getter), std::move(setter));
}

template <typename T>
class CsvMapper {
public:
    Mapper& add(const Column<T>& c) { cols_.push_back(c); return *this; }
    const std::vector<Column<T>>& columns() const { return cols_; }

    std::vector<std::string> to_fields(const T& obj) const {
        std::vector<std::string> out; out.reserve(cols_.size());
        for (auto& c : cols_) out.push_back(c.getter(obj));
        return out;
    }
    bool from_fields(const std::vector<std::string>& fields, T& out) const {
        if (fields.size() < cols_.size()) return false;
        try { for (size_t i=0;i<cols_.size();++i) cols_[i].setter(out, fields[i]); return true; }
        catch (...) { return false; }
    }
private:
    std::vector<Column<T>> cols_;
};

}
