#include "manager/data_manager.hpp"

namespace manager{
    DataManager::DataManager(size_t max_metrics, size_t max_ir)
: max_metrics_(max_metrics), max_ir_(max_ir) {}

void DataManager::add(const Metrics& m) {
    std::lock_guard<std::mutex> lk(mu_m_);
    if (q_m_.size() >= max_metrics_) q_m_.pop_front();
    q_m_.push_back(m);
}
void DataManager::add(const IrSignalLog& l) {
    std::lock_guard<std::mutex> lk(mu_i_);
    if (q_i_.size() >= max_ir_) q_i_.pop_front();
    q_i_.push_back(l);
}

void DataManager::add(const IrSendDevice& d){
    std::lock_guard<std::mutex> lk(mu_d_);
    q_d_.push_back(d);
}

std::vector<Metrics> DataManager::snapshot_metrics() const {
    std::lock_guard<std::mutex> lk(mu_m_);
    return std::vector<Metrics>(q_m_.begin(), q_m_.end());
}
std::vector<IrSignalLog> DataManager::snapshot_ir() const {
    std::lock_guard<std::mutex> lk(mu_i_);
    return std::vector<IrSignalLog>(q_i_.begin(), q_i_.end());
}

template<class T>
std::vector<T> DataManager::_last_n_locked(const std::deque<T>& q, size_t n) {
    if (n >= q.size()) return std::vector<T>(q.begin(), q.end());
    return std::vector<T>(q.end() - static_cast<std::ptrdiff_t>(n), q.end());
}

std::vector<Metrics> DataManager::last_metrics(size_t n) const {
    std::lock_guard<std::mutex> lk(mu_m_);
    return _last_n_locked(q_m_, n);
}
std::vector<IrSignalLog> DataManager::last_ir(size_t n) const {
    std::lock_guard<std::mutex> lk(mu_i_);
    return _last_n_locked(q_i_, n);
}

template<class T, class GetTs>
std::vector<T> DataManager::_between_locked(const std::deque<T>& q, GetTs get_ts,
                                            std::chrono::system_clock::time_point s,
                                            std::chrono::system_clock::time_point e) {
    std::vector<T> out; out.reserve(q.size());
    for (const auto& v : q) {
        auto tp = get_ts(v);
        if (tp >= s && tp <= e) out.push_back(v);
    }
    return out;
}

std::vector<Metrics> DataManager::metrics_between(
    std::chrono::system_clock::time_point s,
    std::chrono::system_clock::time_point e) const
{
    std::lock_guard<std::mutex> lk(mu_m_);
    return _between_locked(q_m_, [](auto& x){ return x.ts; }, s, e);
}

std::vector<IrSignalLog> DataManager::ir_between(
    std::chrono::system_clock::time_point s,
    std::chrono::system_clock::time_point e) const
{
    std::lock_guard<std::mutex> lk(mu_i_);
    return _between_locked(q_i_, [](auto& x){ return x.ts; }, s, e);
}

void DataManager::clear_metrics() { std::lock_guard<std::mutex> lk(mu_m_); q_m_.clear(); }
void DataManager::clear_ir()      { std::lock_guard<std::mutex> lk(mu_i_); q_i_.clear(); }
}