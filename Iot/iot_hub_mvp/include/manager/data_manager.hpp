#pragma once

#include <deque>
#include <mutex>
#include <vector>
#include <chrono>
#include <optional>

#include "types.hpp"
namespace manager{
    class DataManager{
    public:
        explicit DataManager(size_t max_metrics = 10000, size_t max_ir = 10000);

        void add(const Metrics& m);
        void add(const IrSignalLog& i);
        void add(const IrSendDevice& d);

        std::vector<Metrics> snapshot_metrics() const;
        std::vector<IrSignalLog> snapshot_ir() const;

        std::vector<Metrics>      last_metrics(size_t n) const;
        std::vector<IrSignalLog>  last_ir(size_t n) const;

        std::vector<Metrics>      metrics_between(std::chrono::system_clock::time_point start, std::chrono::system_clock::time_point end) const;
        std::vector<IrSignalLog>  ir_between(std::chrono::system_clock::time_point start, std::chrono::system_clock::time_point end) const;

        void clear_metrics();
        void clear_ir();

    private:
        template<class T>
        static std::vector<T> _last_n_locked(const std::deque<T>& q, size_t n);

        template<class T, class GetTs>
        static std::vector<T> _between_locked(const std::deque<T>& q, GetTs get_ts, std::chrono::system_clock::time_point s, std::chrono::system_clock::time_point e);

    private:
        size_t max_metrics_;
        size_t max_ir_;

        mutable std::mutex mu_m_;
        mutable std::mutex mu_i_;
        mutable std::mutex mu_d_;
        std::deque<Metrics>     q_m_;
        std::deque<IrSignalLog> q_i_;
        std::deque<IrSendDevice> q_d_;
    };
}