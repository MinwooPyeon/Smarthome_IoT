#pragma once
#include <vector>
#include <string>
#include <functional>

template <typename T>
struct Column{
    std::string name;
    std::function<std::string(const T&) getter;
    std::function<void(T&, const std::string&)> setter;
};

template <typename T>
class CsvMapper{
public:
    Mapper& add(Column<T> c) { cols_.push_back(std::move(c)); return *this; }
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